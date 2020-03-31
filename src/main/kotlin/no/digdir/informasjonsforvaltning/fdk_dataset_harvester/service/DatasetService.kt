package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.addDefaultPrefixes
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import org.springframework.stereotype.Service

@Service
class DatasetService(private val datasetFuseki: DatasetFuseki) {

    fun getAllDatasets(returnType: JenaType): String =
        datasetFuseki
            .fetchCompleteModel()
            .addDefaultPrefixes()
            .createRDFResponse(returnType)

    fun getDataset(id: String, returnType: JenaType): String? =
        datasetFuseki
            .fetchByGraphName(id)
            ?.addDefaultPrefixes()
            ?.createRDFResponse(returnType)

}