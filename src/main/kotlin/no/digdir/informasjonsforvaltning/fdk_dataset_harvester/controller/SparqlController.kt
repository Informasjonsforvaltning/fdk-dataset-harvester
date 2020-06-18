package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.SparqlService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class SparqlController(private val sparqlService: SparqlService) {

    @GetMapping("/sparql/describe")
    fun sparqlDescribe(@RequestParam(value = "query", required = true) query: String): ResponseEntity<String> =
        try {
            sparqlService.sparqlDescribe(query)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NO_CONTENT)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }

    @GetMapping("/sparql/construct")
    fun sparqlConstruct(@RequestParam(value = "query", required = true) query: String): ResponseEntity<String> =
        try {
            sparqlService.sparqlConstruct(query)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NO_CONTENT)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }

    @GetMapping("/sparql/select")
    fun sparqlSelect(@RequestParam(value = "query", required = true) query: String): ResponseEntity<String> =
        try {
            sparqlService.sparqlSelect(query)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NO_CONTENT)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }

    @GetMapping("/sparql/ask")
    fun sparqlAsk(@RequestParam(value = "query", required = true) query: String): ResponseEntity<Boolean> =
        try {
            ResponseEntity(sparqlService.sparqlAsk(query), HttpStatus.OK)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }

}