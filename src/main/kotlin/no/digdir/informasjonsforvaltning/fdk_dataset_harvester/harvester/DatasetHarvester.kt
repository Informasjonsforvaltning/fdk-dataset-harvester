package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.FusekiAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.MiscellaneousRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.gzip
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.ungzip
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(DatasetHarvester::class.java)

@Service
class DatasetHarvester(
    private val adapter: DatasetAdapter,
    private val catalogRepository: CatalogRepository,
    private val datasetRepository: DatasetRepository,
    private val miscRepository: MiscellaneousRepository,
    private val applicationProperties: ApplicationProperties,
    private val fusekiAdapter: FusekiAdapter
) {

    fun updateUnionModel() {
        var unionModel = ModelFactory.createDefaultModel()

        catalogRepository.findAll()
            .map { parseRDFResponse(ungzip(it.turtleCatalog), Lang.TURTLE, null) }
            .forEach { unionModel = unionModel.union(it) }

        fusekiAdapter.storeUnionModel(unionModel)

        miscRepository.save(
            MiscellaneousTurtle(
                id = UNION_ID,
                isHarvestedSource = false,
                turtle = gzip(unionModel.createRDFResponse(Lang.TURTLE))
            )
        )
    }

    fun harvestDatasetCatalog(source: HarvestDataSource, harvestDate: Calendar) {
        if(source.url != null) {
            LOGGER.debug("Starting harvest of ${source.url}")
            val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

            if (jenaWriterType == null || jenaWriterType == Lang.RDFNULL) {
                LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable ")
            } else {
                val harvested = adapter.getDatasets(source)
                    ?.let { parseRDFResponse(it, jenaWriterType, source.url) }

                if (harvested == null) LOGGER.info("Not able to harvest ${source.url}")
                else {
                    val dbId = createIdFromUri(source.url)
                    val dbData = miscRepository
                        .findByIdOrNull(source.url)
                        ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }

                    if (dbData != null && harvested.isIsomorphicWith(dbData)) LOGGER.info("No changes from last harvest of ${source.url}")
                    else {
                        LOGGER.info("Changes detected, saving data from ${source.url} on graph $dbId, and updating FDK meta data")

                        miscRepository.save(
                            MiscellaneousTurtle(
                                id = source.url,
                                isHarvestedSource = true,
                                turtle = gzip(harvested.createRDFResponse(Lang.TURTLE))
                            )
                        )

                        updateDB(harvested, harvestDate)
                    }
                }
            }
        }
    }

    private fun updateDB(harvested: Model, harvestDate: Calendar) {
        val catalogsToSave = mutableListOf<CatalogDBO>()
        val datasetsToSave = mutableListOf<DatasetDBO>()

        splitCatalogsFromModel(harvested)
            .map { Pair(it, catalogRepository.findByIdOrNull(it.resource.uri)) }
            .filter { it.first.catalogDiffersFromDB(it.second) }
            .forEach {
                val catalogURI = it.first.resource.uri

                val fdkId = it.second?.fdkId ?: createIdFromUri(catalogURI)
                val resourceUri = "${applicationProperties.catalogUri}/$fdkId"

                val issued = it.second?.issued
                    ?.let { timestamp -> calendarFromTimestamp(timestamp) }
                    ?: harvestDate

                var catalogModel = it.first.harvestedCatalogWithoutDatasets

                catalogModel.createResource(resourceUri)
                    .addProperty(RDF.type, DCAT.CatalogRecord)
                    .addProperty(DCTerms.identifier, fdkId)
                    .addProperty(FOAF.primaryTopic, catalogModel.createResource(catalogURI))
                    .addProperty(DCTerms.issued, catalogModel.createTypedLiteral(issued))
                    .addProperty(DCTerms.modified, catalogModel.createTypedLiteral(harvestDate))

                val datasetsWithIsChanged = it.first.datasets
                    .map { dataset ->
                        val dbDataset = datasetRepository.findByIdOrNull(dataset.resource.uri)
                        if (dbDataset == null || dataset.differsFromDB(dbDataset)) {
                            Pair(dataset.mapToUpdatedDBO(harvestDate, resourceUri, dbDataset), true)
                        } else {
                            Pair(dbDataset, false)
                        }
                    }

                datasetsWithIsChanged
                    .map { pair -> pair.first }
                    .map { dataset -> parseRDFResponse(ungzip(dataset.turtleDataset), Lang.TURTLE, null) }
                    .forEach { model -> catalogModel = catalogModel.union(model) }

                datasetsWithIsChanged
                    .filter { dsWithChanged -> dsWithChanged.second }
                    .forEach { dsPair -> datasetsToSave.add(dsPair.first) }

                catalogsToSave.add(
                    CatalogDBO(
                        uri = catalogURI,
                        fdkId = fdkId,
                        issued = issued.timeInMillis,
                        modified = harvestDate.timeInMillis,
                        turtleHarvested = gzip(it.first.harvestedCatalog.createRDFResponse(Lang.TURTLE)),
                        turtleCatalog = gzip(catalogModel.createRDFResponse(Lang.TURTLE))
                    )
                )
            }

        catalogRepository.saveAll(catalogsToSave)
        datasetRepository.saveAll(datasetsToSave)
    }

    private fun DatasetModel.mapToUpdatedDBO(harvestDate: Calendar, catalogURI: String, dbDataset: DatasetDBO?): DatasetDBO {
        val fdkId = dbDataset?.fdkId ?: createIdFromUri(resource.uri)

        val metaModel = ModelFactory.createDefaultModel()
        metaModel.addDefaultPrefixes()

        val issued: Calendar = dbDataset?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        metaModel.createResource("${applicationProperties.datasetUri}/$fdkId")
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(resource.uri))
            .addProperty(DCTerms.isPartOf, metaModel.createResource(catalogURI))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(issued))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(harvestDate))

        return DatasetDBO (
            uri = resource.uri,
            fdkId = fdkId,
            isPartOf = catalogURI,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis,
            turtleHarvested = gzip(harvestedDataset.createRDFResponse(Lang.TURTLE)),
            turtleDataset = gzip(metaModel.union(harvestedDataset).createRDFResponse(Lang.TURTLE))
        )
    }
}