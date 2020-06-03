package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.HarvestFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.MetaFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.addDefaultPrefixes
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createModel
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractMetaDataTopic
import org.apache.jena.rdf.model.Model
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(DatasetService::class.java)

@Service
class DatasetService(
    private val metaFuseki: MetaFuseki,
    private val harvestFuseki: HarvestFuseki,
    private val applicationProperties: ApplicationProperties
) {

    fun countMetaData(): Int =
        metaFuseki.fetchCompleteModel()
            .listResourcesWithProperty(RDF.type, DCAT.CatalogRecord)
            .toList()
            .size

    fun getAll(returnType: JenaType): String =
        harvestFuseki.fetchCompleteModel()
            .union(metaFuseki.fetchCompleteModel())
            .addDefaultPrefixes()
            .createRDFResponse(returnType)

    fun getDataset(id: String, returnType: JenaType): String? {
        val query = "DESCRIBE <${applicationProperties.datasetUri}/$id>"
        LOGGER.info(query)
        return metaFuseki.queryDescribe(query)
            ?.let { metaData ->
                val topicURI = metaData.extractMetaDataTopic()
                if (topicURI != null) metaData.union(getByURI(topicURI))
                else null
            }
            ?.addDefaultPrefixes()
            ?.createRDFResponse(returnType)
    }

    fun getDatasetCatalog(id: String, returnType: JenaType): String? {
        val query = "DESCRIBE <${applicationProperties.catalogUri}/$id>"
        LOGGER.info(query)
        return metaFuseki.queryDescribe(query)
            ?.let { metaData ->
                val topicURI = metaData.extractMetaDataTopic()
                if (topicURI != null) metaData.union(getByURI(topicURI))
                else null
            }
            ?.addDefaultPrefixes()
            ?.createRDFResponse(returnType)
    }

    private fun getByURI(uri: String): Model =
        harvestFuseki.fetchCompleteModel()
            .getResource(uri)
            .createModel()

}