package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.FusekiProperties
import org.apache.jena.query.ReadWrite
import org.apache.jena.query.ResultSet
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdfconnection.RDFConnection
import org.apache.jena.rdfconnection.RDFConnectionFuseki
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

private val LOGGER = LoggerFactory.getLogger(MetaFuseki::class.java)

@Service
class MetaFuseki(private val fusekiProperties: FusekiProperties) {

    private fun metaConnection(): RDFConnection =
        RDFConnectionFuseki.create()
            .destination(this.fusekiProperties.metaUri)
            .queryEndpoint("${this.fusekiProperties.metaUri}/query")
            .updateEndpoint("${this.fusekiProperties.metaUri}/update")
            .build()

    fun fetchCompleteModel(): Model =
        metaConnection().use {
            it.begin(ReadWrite.READ)
            return it.fetchDataset().unionModel
        }

    fun fetchByGraphName(graphName: String): Model? =
        metaConnection().use {
            it.begin(ReadWrite.READ)
            return try {
                it.fetch(graphName)
            } catch (ex: Exception) {
                null
            }
        }

    fun queryDescribe(query: String): Model? =
        metaConnection().use {
            it.begin(ReadWrite.READ)
            return try {
                it.queryDescribe(query)
            } catch (ex: Exception) {
                LOGGER.error("sparql exception: $ex")
                null
            }
        }

    fun querySelect(query: String, format: QueryResponseFormat): String? =
        metaConnection().use {
            it.begin(ReadWrite.READ)
            var result: String? = null
            try {
                it.queryResultSet(query) {
                    qs ->
                    result = when (format) {
                        QueryResponseFormat.TEXT -> ResultSetFormatter.asText(qs).valuesOrNull(format)
                        QueryResponseFormat.JSON -> querySelectWithJsonResponse(qs)?.valuesOrNull(format)
                        QueryResponseFormat.XML -> ResultSetFormatter.asXMLString(qs).valuesOrNull(format)
                    }
                }
            } catch (ex : Exception) {
                LOGGER.error("sparql-select exception: $ex")
                throw ex
            }
            return result
        }

    private fun querySelectWithJsonResponse(qs: ResultSet): String? {
        val outputStream = ByteArrayOutputStream()
        ResultSetFormatter.outputAsJSON(outputStream,qs)
        return String(outputStream.toByteArray())
    }

    fun saveWithGraphName(graphName: String, model: Model) =
        metaConnection().use {
            it.begin(ReadWrite.WRITE)
            it.put(graphName, model)
        }

}

private fun String.valuesOrNull(format: QueryResponseFormat): String? =
    when(format){
        QueryResponseFormat.TEXT -> if (this.split('\n').size > 5) this else null
        QueryResponseFormat.JSON -> if (this.split("\"bindings\": [")[1]
                .replace(Regex("[^A-Za-z0-9]"), "")
                .isNotEmpty()) this else null
        QueryResponseFormat.XML -> if (this.split("<results>")[1]
                .split("</results>")[0]
                .replace(Regex("[^A-Za-z0-9]"), "")
                .isNotEmpty()) this else null
    }