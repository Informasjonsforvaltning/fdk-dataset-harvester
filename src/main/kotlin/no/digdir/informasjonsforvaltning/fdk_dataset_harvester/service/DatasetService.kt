package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import org.springframework.stereotype.Service

@Service
class DatasetService(private val datasetFuseki: DatasetFuseki) {

    fun getAllDataServices(returnType: JenaType): String =
        datasetFuseki
            .fetchCompleteModel()
            .createRDFResponse(returnType)

    fun getDataService(id: String, returnType: JenaType): String? =
        datasetFuseki
            .fetchByGraphName(id)
            ?.createRDFResponse(returnType)


}