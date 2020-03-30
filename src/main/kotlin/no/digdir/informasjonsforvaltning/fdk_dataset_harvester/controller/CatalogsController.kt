package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.generated.api.DcatApNoCatalogsApi
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.CatalogService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(CatalogsController::class.java)

@Controller
open class CatalogsController(private val catalogService: CatalogService) : DcatApNoCatalogsApi {

    override fun getCatalogById(httpServletRequest: HttpServletRequest, id: String): ResponseEntity<String> {
        LOGGER.info("get DataService catalog with id $id")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_ACCEPTABLE) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            catalogService.getDatasetCatalog(id, returnType ?: JenaType.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    override fun getCatalogs(httpServletRequest: HttpServletRequest): ResponseEntity<String> {
        LOGGER.info("get all DataService catalogs")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_ACCEPTABLE) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(catalogService.getAllDatasetCatalogs(returnType ?: JenaType.TURTLE), HttpStatus.OK)
    }
}