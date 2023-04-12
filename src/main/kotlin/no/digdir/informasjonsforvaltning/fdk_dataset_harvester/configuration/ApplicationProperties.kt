package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class ApplicationProperties(
    val datasetUri: String,
    val catalogUri: String,
    val harvestAdminRootUrl: String
)
