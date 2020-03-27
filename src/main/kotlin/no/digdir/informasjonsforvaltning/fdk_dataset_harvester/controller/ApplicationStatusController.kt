package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.CatalogService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationStatusController(private val catalogService: CatalogService) {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/ready")
    fun ready(): ResponseEntity<Void> {
        try {
            catalogService.countDatasetCatalogs()
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Int> {
        try {
            return ResponseEntity.ok(catalogService.countDatasetCatalogs())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

}