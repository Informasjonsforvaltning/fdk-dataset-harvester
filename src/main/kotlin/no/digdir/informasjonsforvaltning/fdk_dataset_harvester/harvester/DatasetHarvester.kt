package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.dto.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createDatasetModel
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createModelOfTopLevelProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractCatalogModelURI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractDatasetModelURI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractMetaDataIdentifier
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.parseRDFResponse
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(DatasetHarvester::class.java)

@Service
class DatasetHarvester(
    private val adapter: DatasetAdapter,
    private val catalogFuseki: CatalogFuseki,
    private val datasetFuseki: DatasetFuseki,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestDataServiceCatalog(source: HarvestDataSource, harvestDate: Calendar) {
        LOGGER.debug("Starting harvest of ${source.url}")
        val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

        if (jenaWriterType == null || jenaWriterType == JenaType.NOT_ACCEPTABLE) {
            LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable ")
        } else {
            adapter.getDataServiceCatalog(source)
                ?.let { parseRDFResponse(it, jenaWriterType) }
                ?.addMetaData(harvestDate)
                ?.run {
                    catalogs.forEach {
                        val modelId = it.extractMetaDataIdentifier()
                        catalogFuseki.saveWithGraphName(modelId, it)
                        LOGGER.debug("Updated catalog model ${it.extractCatalogModelURI()}, id: $modelId")
                    }

                    dataServices.forEach {
                        val modelId = it.extractMetaDataIdentifier()
                        datasetFuseki.saveWithGraphName(modelId, it)
                        LOGGER.debug("Updated data service model ${it.extractDatasetModelURI()}, id: $modelId")
                    }
                }
        }
    }

    private fun catalogModelWithMetaData(resource: Resource, harvestDate: Calendar): Model {
        val dbId = createIdFromUri(resource.uri)
        val dbModel = catalogFuseki.fetchByGraphName(dbId)

        val dbMetaData: Resource? = dbModel?.extractMetaDataResource()

        return resource.createModelOfTopLevelProperties()
            .addCatalogMetaData(dbId, resource.uri, dbMetaData, harvestDate)
    }

    private fun datasetModelWithMetaData(resource: Resource, harvestDate: Calendar): Model {
        val dbId = createIdFromUri(resource.uri)
        val dbModel = datasetFuseki.fetchByGraphName(dbId)

        val dbMetaData: Resource? = dbModel?.extractMetaDataResource()

        return resource.createDatasetModel()
            .addDataServiceMetaData(dbId, resource.uri, dbMetaData, harvestDate)
    }

    private fun Model.addCatalogMetaData(dbId: String, uri: String, dbMetaData: Resource?, harvestDate: Calendar): Model {
        createResource("${applicationProperties.catalogUri}/$dbId")
            .addProperty(RDF.type, DCAT.record)
            .addProperty(DCTerms.identifier, dbId)
            .addProperty(FOAF.primaryTopic, createResource(uri))
            .addProperty(DCTerms.issued, issuedDate(dbMetaData, harvestDate))
            .addModified(dbMetaData, harvestDate)
        return this
    }

    private fun Model.addDataServiceMetaData(dbId: String, uri: String, dbMetaData: Resource?, harvestDate: Calendar): Model {
        createResource("${applicationProperties.datasetUri}/$dbId")
            .addProperty(RDF.type, DCAT.record)
            .addProperty(DCTerms.identifier, dbId)
            .addProperty(FOAF.primaryTopic, createResource(uri))
            .addProperty(DCTerms.issued,  issuedDate(dbMetaData, harvestDate))
            .addModified(dbMetaData, harvestDate)

        return this
    }

    private fun Model.addMetaData(harvestDate: Calendar): Models {
        val catalogModels = mutableListOf<Model>()
        val datasetModels = mutableListOf<Model>()

        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .forEach {
                catalogModels.add(catalogModelWithMetaData(it, harvestDate))
            }

        listResourcesWithProperty(RDF.type, DCAT.Dataset)
            .toList()
            .forEach {
                datasetModels.add(datasetModelWithMetaData(it, harvestDate))
            }

        return Models(catalogModels, datasetModels)
    }
}

private data class Models(
    val catalogs: List<Model>,
    val dataServices: List<Model>
)

private fun Model.extractMetaDataResource(): Resource? =
    listResourcesWithProperty(RDF.type, DCAT.record)
        .toList()
        .let { if (it.isNotEmpty()) it.first() else null }

private fun Model.issuedDate(dbResource: Resource?, harvestDate: Calendar): Literal =
    dbResource?.listProperties(DCTerms.issued)
        ?.toList()
        ?.firstOrNull()
        ?.literal
        ?: createTypedLiteral(harvestDate)

private fun Resource.addModified(dbResource: Resource?, harvestDate: Calendar) {
    addProperty(DCTerms.modified, model.createTypedLiteral(harvestDate))

    dbResource?.listProperties(DCTerms.modified)
        ?.toList()
        ?.forEach { addProperty(DCTerms.modified, it.string) }
}