package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.HarvestFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.MetaFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.MissingHarvestException
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.addDefaultPrefixes
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.extractMetaDataTopic
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
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
                if (topicURI != null) metaData.union(datasetByURI(topicURI))
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
                if (topicURI != null) metaData.union(catalogByURI(topicURI))
                else null
            }
            ?.addDefaultPrefixes()
            ?.createRDFResponse(returnType)
    }

    private fun catalogByURI(uri: String): Model {
        val literalsQuery = "DESCRIBE <$uri>"
        val propertiesQuery = "DESCRIBE * WHERE { <$uri> ?p ?o }"

        val datasetProperties = "dcat:dataset/dcat:distribution|dcat:dataset/dcat:distribution/dcatapi:accessService|dcat:dataset/dct:publisher|dcat:dataset/dcat:contactPoint|dcat:dataset/dct:spatial"
        val queryPrefixes = "PREFIX dcat: <http://www.w3.org/ns/dcat#> PREFIX dct: <http://purl.org/dc/terms/> PREFIX dcatapi: <http://dcat.no/dcatapi/>"
        val datasetPropertiesQuery = "$queryPrefixes DESCRIBE * WHERE { <$uri> $datasetProperties ?o }"

        val harvestedData = harvestFuseki.queryDescribe(literalsQuery)
            ?.union(harvestFuseki.queryDescribe(propertiesQuery) ?: ModelFactory.createDefaultModel())
            ?.union(harvestFuseki.queryDescribe(datasetPropertiesQuery) ?: ModelFactory.createDefaultModel())

        if (harvestedData == null) throw MissingHarvestException()
        else return harvestedData
    }

    private fun datasetByURI(uri: String): Model {
        val literalsQuery = "DESCRIBE <$uri>"
        val propertiesQuery = "DESCRIBE * WHERE { <$uri> ?p ?o }"

        val distributionProperties = "dcat:distribution/dcatapi:accessService"
        val queryPrefixes = "PREFIX dcat: <http://www.w3.org/ns/dcat#> PREFIX dcatapi: <http://dcat.no/dcatapi/>"
        val distributionPropertiesQuery = "$queryPrefixes DESCRIBE * WHERE { <$uri> $distributionProperties ?o }"

        val harvestedData = harvestFuseki.queryDescribe(literalsQuery)
            ?.union(harvestFuseki.queryDescribe(propertiesQuery) ?: ModelFactory.createDefaultModel())
            ?.union(harvestFuseki.queryDescribe(distributionPropertiesQuery) ?: ModelFactory.createDefaultModel())

        if (harvestedData == null) throw MissingHarvestException()
        else return harvestedData
    }

}