package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester.extractCatalogModel
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val applicationProperties: ApplicationProperties,
    private val catalogRepository: CatalogRepository,
    private val datasetRepository: DatasetRepository,
    private val turtleService: TurtleService
) {

    fun updateUnionModels() {
        val catalogUnion = ModelFactory.createDefaultModel()
        val noRecordsUnion = ModelFactory.createDefaultModel()

        catalogRepository.findAll()
            .forEach {
                turtleService.getCatalog(it.fdkId, withRecords = true)
                    ?.let { turtle -> safeParseRDF(turtle, Lang.TURTLE) }
                    ?.run { catalogUnion.add(this) }

                turtleService.getCatalog(it.fdkId, withRecords = false)
                    ?.let { turtle -> safeParseRDF(turtle, Lang.TURTLE) }
                    ?.run { noRecordsUnion.add(this) }
            }

        turtleService.saveAsCatalogUnion(catalogUnion, true)
        turtleService.saveAsCatalogUnion(noRecordsUnion, false)
    }

    fun updateMetaData() {
        catalogRepository.findAll()
            .forEach { catalog ->
                val catalogNoRecords = turtleService.getCatalog(catalog.fdkId, withRecords = false)
                    ?.let { safeParseRDF(it, Lang.TURTLE) }

                if (catalogNoRecords != null) {
                    val catalogURI = "${applicationProperties.catalogUri}/${catalog.fdkId}"
                    val catalogMeta = catalog.createMetaModel()
                    val completeMetaModel = ModelFactory.createDefaultModel()
                    completeMetaModel.add(catalogMeta)

                    val catalogTriples = catalogNoRecords.getResource(catalog.uri)
                        .extractCatalogModel()
                    catalogTriples.add(catalogMeta)

                    datasetRepository.findAllByIsPartOf(catalogURI)
                        .filter { !it.removed }
                        .filter { it.modelContainsDataset(catalogNoRecords) }
                        .forEach { dataset ->
                            val datasetMeta = dataset.createMetaModel()
                            catalogMeta.add(datasetMeta)

                            turtleService.getDataset(dataset.fdkId, withRecords = false)
                                ?.let { datasetNoRecords -> safeParseRDF(datasetNoRecords, Lang.TURTLE) }
                                ?.let { datasetModelNoRecords -> datasetMeta.union(datasetModelNoRecords) }
                                ?.let { datasetModel -> catalogTriples.union(datasetModel) }
                                ?.run { turtleService.saveAsDataset(this, fdkId = dataset.fdkId, withRecords = true) }
                        }

                    turtleService.saveAsCatalog(
                        catalogMeta.union(catalogNoRecords),
                        fdkId = catalog.fdkId,
                        withRecords = true
                    )
                }
            }

        updateUnionModels()
    }

    fun updateMetaDataAndCatalogs() {
        catalogRepository.findAll()
            .forEach { catalog ->
                val dbCatalog = turtleService.getCatalog(catalog.fdkId, withRecords = false)
                    ?.let { safeParseRDF(it, Lang.TURTLE) }

                if (dbCatalog != null) {
                    val catalogURI = "${applicationProperties.catalogUri}/${catalog.fdkId}"
                    val catalogModel = dbCatalog.getResource(catalog.uri)
                        .extractCatalogModel()

                    datasetRepository.findAllByIsPartOf(catalogURI)
                        .filter { !it.removed }
                        .filter { it.modelContainsDataset(dbCatalog) }
                        .forEach { dataset ->
                            turtleService.getDataset(dataset.fdkId, withRecords = false)
                                ?.let { datasetNoRecords -> safeParseRDF(datasetNoRecords, Lang.TURTLE) }
                                ?.let { datasetModel -> catalogModel.add(datasetModel) }
                        }

                    turtleService.saveAsCatalog(
                        catalogModel,
                        fdkId = catalog.fdkId,
                        withRecords = false
                    )
                }
            }

        updateMetaData()
    }

    private fun CatalogMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.catalogUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()
        metaModel.addMetaPrefixes()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun DatasetMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.datasetUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()
        metaModel.addMetaPrefixes()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.isPartOf, metaModel.createResource(isPartOf))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun DatasetMeta.modelContainsDataset(model: Model): Boolean =
        model.containsTriple("<${uri}>", "a", "<${DCAT.Dataset.uri}>")
            || model.containsTriple("<${uri}>", "a", "<${DCAT3.DatasetSeries.uri}>")

}
