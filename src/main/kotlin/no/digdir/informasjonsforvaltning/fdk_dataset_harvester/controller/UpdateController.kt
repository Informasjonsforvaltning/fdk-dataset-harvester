package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.controller

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.EndpointPermissions
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.UpdateService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/update")
class UpdateController(
    private val endpointPermissions: EndpointPermissions,
    private val updateService: UpdateService
) {

    @PostMapping("/meta")
    fun updateMetaData(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Void> =
        if (endpointPermissions.hasAdminPermission(jwt)) {
            updateService.updateMetaData()
            ResponseEntity(HttpStatus.OK)
        } else ResponseEntity(HttpStatus.FORBIDDEN)

}
