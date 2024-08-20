package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DuplicateIRI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.DatasetService
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.EndpointPermissions
import org.apache.jena.riot.Lang
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@CrossOrigin
@RequestMapping(
    value = ["/datasets"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
open class DatasetsController(
    private val datasetService: DatasetService,
    private val endpointPermissions: EndpointPermissions
) {

    @GetMapping("/{id}")
    fun getDatasetById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String,
        @RequestParam(value = "catalogrecords", required = false) catalogRecords: Boolean = false
    ): ResponseEntity<String> {
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            datasetService.getDataset(id, returnType ?: Lang.TURTLE, catalogRecords)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/{id}/remove")
    fun removeDatasetById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: String
    ): ResponseEntity<Void> =
    if (endpointPermissions.hasAdminPermission(jwt)) {
        datasetService.removeDataset(id)
        ResponseEntity(HttpStatus.OK)
    } else ResponseEntity(HttpStatus.FORBIDDEN)

    @DeleteMapping("/{id}")
    fun purgeDatasetById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: String
    ): ResponseEntity<Void> =
        if (endpointPermissions.hasAdminPermission(jwt)) {
            datasetService.purgeByFdkId(id)
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else ResponseEntity(HttpStatus.FORBIDDEN)

    @PostMapping("/remove-duplicates")
    fun removeDuplicates(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody duplicates: List<DuplicateIRI>
    ): ResponseEntity<Void> =
        if (endpointPermissions.hasAdminPermission(jwt)) {
            datasetService.removeDuplicates(duplicates)
            ResponseEntity(HttpStatus.OK)
        } else ResponseEntity(HttpStatus.FORBIDDEN)

}
