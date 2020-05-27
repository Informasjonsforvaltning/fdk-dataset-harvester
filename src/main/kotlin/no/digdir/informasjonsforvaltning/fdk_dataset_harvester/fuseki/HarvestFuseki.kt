package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.FusekiProperties
import org.apache.jena.query.ReadWrite
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdfconnection.RDFConnection
import org.apache.jena.rdfconnection.RDFConnectionFuseki
import org.springframework.stereotype.Service

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

    fun queryDescribe(query: String): Model? =
        datasetConnection().use {
            it.begin(ReadWrite.READ)
            return try {
                it.queryDescribe(query)
            } catch (ex: Exception) {
                null
            }
        }

    fun saveWithGraphName(graphName: String, model: Model) =
        datasetConnection().use {
            it.begin(ReadWrite.WRITE)
            it.put(graphName, model)
        }

}