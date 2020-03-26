package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.generated.api.DcatApNoDatasetsApi
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(DataservicesController::class.java)

@Controller
open class DataservicesController() : DcatApNoDatasetsApi {

    override fun getDatasetById(httpServletRequest: HttpServletRequest, id: String): ResponseEntity<String> {
        LOGGER.info("get Dataset with id $id")

        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    override fun getDatasets(httpServletRequest: HttpServletRequest): ResponseEntity<String> {
        LOGGER.info("get all Datasets")

        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}