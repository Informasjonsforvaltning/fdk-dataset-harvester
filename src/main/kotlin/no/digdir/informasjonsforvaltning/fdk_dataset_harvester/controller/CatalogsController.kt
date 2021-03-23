package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.DatasetService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(CatalogsController::class.java)

@Controller
@CrossOrigin
@RequestMapping(
    value = ["/catalogs"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml", "application/n-triples"]
)
open class CatalogsController(private val datasetService: DatasetService) {

    @GetMapping("/{id}")
    fun getCatalogById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String,
        @RequestParam(value = "catalogrecords", required = false) catalogRecords: Boolean = false
    ): ResponseEntity<String> {
        LOGGER.info("get dataset catalog with id $id")
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            datasetService.getDatasetCatalog(id, returnType ?: Lang.TURTLE, catalogRecords)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getCatalogs(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @RequestParam(value = "catalogrecords", required = false) catalogRecords: Boolean = false
    ): ResponseEntity<String> {
        LOGGER.info("get all dataset catalogs")
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(datasetService.getAll(returnType ?: Lang.TURTLE, catalogRecords), HttpStatus.OK)
    }
}
