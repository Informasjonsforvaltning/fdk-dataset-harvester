package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationStatusController() {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/ready")
    fun ready(): ResponseEntity<Void> {
        try {
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Int> {
        try {
            return ResponseEntity.ok(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

}