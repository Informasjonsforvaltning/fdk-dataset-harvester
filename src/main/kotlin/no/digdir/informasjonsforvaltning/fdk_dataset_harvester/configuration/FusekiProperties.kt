package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("fuseki")
data class FusekiProperties(
    val datasetUri: String,
    val metaUri: String
)