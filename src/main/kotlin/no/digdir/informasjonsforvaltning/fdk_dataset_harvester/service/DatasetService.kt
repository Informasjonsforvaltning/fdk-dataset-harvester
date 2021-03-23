package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service

@Service
class DatasetService(
    private val turtleService: TurtleService,
) {

    fun getAll(returnType: Lang, withRecords: Boolean): String =
        turtleService.getCatalogUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getDataset(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getDataset(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

    fun getDatasetCatalog(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getCatalog(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

}
