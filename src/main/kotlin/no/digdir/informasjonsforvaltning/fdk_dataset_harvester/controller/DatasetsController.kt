package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.generated.api.DcatApNoDatasetsApi
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

        return datasetService.getDataService(id)
            ?.let { ResponseEntity(it, HttpStatus.OK) }
            ?: ResponseEntity(HttpStatus.NOT_FOUND)
    }

    override fun getDatasets(httpServletRequest: HttpServletRequest): ResponseEntity<String> {
        LOGGER.info("get all Datasets")

        return ResponseEntity(datasetService.getAllDataServices(), HttpStatus.OK)
    }
}