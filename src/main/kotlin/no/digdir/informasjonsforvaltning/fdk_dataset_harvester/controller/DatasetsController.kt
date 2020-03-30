package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.generated.api.DcatApNoDatasetsApi
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.DatasetService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(DatasetsController::class.java)

@Controller
open class DatasetsController(private val datasetService: DatasetService) : DcatApNoDatasetsApi {

    override fun getDatasetById(httpServletRequest: HttpServletRequest, id: String): ResponseEntity<String> {
        LOGGER.info("get Dataset with id $id")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_ACCEPTABLE) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            datasetService.getDataService(id, returnType ?: JenaType.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    override fun getDatasets(httpServletRequest: HttpServletRequest): ResponseEntity<String> {
        LOGGER.info("get all Datasets")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_ACCEPTABLE) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(datasetService.getAllDataServices(returnType ?: JenaType.TURTLE), HttpStatus.OK)
    }
}