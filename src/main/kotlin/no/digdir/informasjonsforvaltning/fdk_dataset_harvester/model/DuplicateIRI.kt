package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

data class DuplicateIRI(
    val iriToRetain: String,
    val iriToRemove: String,
    val keepRemovedFdkId: Boolean = true
)
