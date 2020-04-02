package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.dto.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createModel
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractCatalogModelURI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractDatasetModelURI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractMetaDataIdentifier
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.parseRDFResponse
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
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

    fun harvestDatasetCatalog(source: HarvestDataSource, harvestDate: Calendar) {
        LOGGER.debug("Starting harvest of ${source.url}")
        val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

        if (jenaWriterType == null || jenaWriterType == JenaType.NOT_ACCEPTABLE) {
            LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable ")
        } else {
            adapter.getDatasetCatalog(source)
                ?.let { parseRDFResponse(it, jenaWriterType) }
                ?.filterModifiedAndAddMetaData(harvestDate)
                ?.run {
                    catalogs.forEach {
                        val modelId = it.extractMetaDataIdentifier()
                        catalogFuseki.saveWithGraphName(modelId, it)
                        LOGGER.debug("Updated catalog model ${it.extractCatalogModelURI()}, id: $modelId")
                    }

                    datasets.forEach {
                        val modelId = it.extractMetaDataIdentifier()
                        datasetFuseki.saveWithGraphName(modelId, it)
                        LOGGER.debug("Updated data service model ${it.extractDatasetModelURI()}, id: $modelId")
                    }
                }
        }
    }

    private fun catalogModelWithMetaData(resource: Resource, harvestDate: Calendar): HarvestedModel {
        val dbId = createIdFromUri(resource.uri)
        val dbModel = catalogFuseki.fetchByGraphName(dbId)
        val harvested = resource.createModel()

        val dbMetaData: Resource? = dbModel?.extractMetaDataResource()

        val isModified = !harvestedIsIsomorphicWithDatabaseModel(dbModel, harvested, dbMetaData?.listProperties()?.toModel())

        val updatedModel = if (!isModified && dbModel != null) {
            LOGGER.debug("No changes detected in catalog model ${resource.uri}")
            dbModel
        } else harvested.addCatalogMetaData(dbId, resource.uri, dbMetaData, harvestDate)

        return HarvestedModel(updatedModel, isModified)
    }

    private fun datasetModelWithMetaData(resource: Resource, harvestDate: Calendar): HarvestedModel {
        val dbId = createIdFromUri(resource.uri)
        val dbModel = datasetFuseki.fetchByGraphName(dbId)
        val harvested = resource.createModel()

        val dbMetaData: Resource? = dbModel?.extractMetaDataResource()

        val isModified = !harvestedIsIsomorphicWithDatabaseModel(dbModel, harvested, dbMetaData?.listProperties()?.toModel())

        val updatedModel = if (!isModified && dbModel != null) {
            LOGGER.debug("No changes detected in data service model ${resource.uri}")
            dbModel
        } else harvested.addDatasetMetaData(dbId, resource.uri, dbMetaData, harvestDate)

        return HarvestedModel(updatedModel, isModified)
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

    private fun Model.addDatasetMetaData(dbId: String, uri: String, dbMetaData: Resource?, harvestDate: Calendar): Model {
        createResource("${applicationProperties.datasetUri}/$dbId")
            .addProperty(RDF.type, DCAT.record)
            .addProperty(DCTerms.identifier, dbId)
            .addProperty(FOAF.primaryTopic, createResource(uri))
            .addProperty(DCTerms.issued,  issuedDate(dbMetaData, harvestDate))
            .addModified(dbMetaData, harvestDate)

        return this
    }

    private fun Model.filterModifiedAndAddMetaData(harvestDate: Calendar): ModifiedModels {
        val catalogModels = mutableListOf<HarvestedModel>()
        val datasetModels = mutableListOf<HarvestedModel>()

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

        return ModifiedModels(
            catalogModels
                .toList()
                .filter { it.isModified }
                .map { it.model },
            datasetModels
                .toList()
                .filter { it.isModified }
                .map { it.model }
        )
    }
}

private data class HarvestedModel(
    val model: Model,
    val isModified: Boolean
)

private data class ModifiedModels(
    val catalogs: List<Model>,
    val datasets: List<Model>
)

private fun Model.extractMetaDataResource(): Resource? =
    listResourcesWithProperty(RDF.type, DCAT.record)
        .toList()
        .let { if (it.isNotEmpty()) it.first() else null }

private fun harvestedIsIsomorphicWithDatabaseModel(fullModelFromDB: Model?, harvestedModel: Model, metaDataModelFromDB: Model?): Boolean =
    fullModelFromDB?.isIsomorphicWith(
        harvestedModel.union(
            metaDataModelFromDB ?: ModelFactory.createDefaultModel()
        )
    ) ?: false

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