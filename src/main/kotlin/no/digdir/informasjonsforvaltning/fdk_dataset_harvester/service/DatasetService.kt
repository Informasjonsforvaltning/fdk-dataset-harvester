package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import org.springframework.stereotype.Service

@Service
class DatasetService(private val datasetFuseki: DatasetFuseki) {

    fun getAllDataServices(): String =
        datasetFuseki
            .fetchCompleteModel()
            .createRDFResponse()

    fun getDataService(id: String): String? =
        datasetFuseki
            .fetchByGraphName(id)
            ?.createRDFResponse()


}