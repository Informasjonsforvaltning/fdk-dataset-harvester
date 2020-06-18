package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.FusekiProperties
import org.apache.jena.query.ReadWrite
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdfconnection.RDFConnection
import org.apache.jena.rdfconnection.RDFConnectionFuseki
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(HarvestFuseki::class.java)

@Service
class HarvestFuseki(private val fusekiProperties: FusekiProperties) {

    private fun datasetConnection(): RDFConnection =
        RDFConnectionFuseki.create()
            .destination(this.fusekiProperties.datasetUri)
            .queryEndpoint("${this.fusekiProperties.datasetUri}/query")
            .updateEndpoint("${this.fusekiProperties.datasetUri}/update")
            .build()

    fun fetchCompleteModel(): Model =
        datasetConnection().use {
            it.begin(ReadWrite.READ)
            return it.fetchDataset().unionModel
        }

    fun fetchByGraphName(graphName: String): Model? =
        datasetConnection ().use {
            it.begin(ReadWrite.READ)
            return try {
                it.fetch(graphName)
            } catch (ex: Exception) {
                null
            }
        }

    fun queryAsk(query: String): Boolean =
        datasetConnection().use {
            it.begin(ReadWrite.READ)
            try {
                it.queryAsk(query)
            } catch (ex: Exception) {
                LOGGER.error("sparql-ask exception: $ex")
                false
            }
        }

    fun queryConstruct(query: String): Model? =
        datasetConnection().use {
            it.begin(ReadWrite.READ)
            try {
                it.queryConstruct(query)
            } catch (ex: Exception) {
                LOGGER.error("sparql-construct exception: $ex")
                null
            }
        }

    fun queryDescribe(query: String): Model? =
        datasetConnection().use {
            it.begin(ReadWrite.READ)
            return try {
                it.queryDescribe(query)
            } catch (ex: Exception) {
                LOGGER.error("sparql-describe exception: $ex")
                null
            }
        }

    fun querySelect(query: String): String? =
        datasetConnection().use {
            it.begin(ReadWrite.READ)
            var result: String? = null
            try {
                it.queryResultSet(query) {qs -> result = ResultSetFormatter.asText(qs) }
            } catch (ex: Exception) {
                LOGGER.error("sparql-select exception: $ex")
            }
            return result
        }

    fun saveWithGraphName(graphName: String, model: Model) =
        datasetConnection().use {
            it.begin(ReadWrite.WRITE)
            it.put(graphName, model)
        }

}