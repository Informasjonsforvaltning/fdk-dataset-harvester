package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestReport(
    val id: String,
    val url: String,
    val dataType: String = "dataset",
    val harvestError: Boolean,
    val start: String,
    val end: String,
    val errorMessage: String? = null,
    val changedCatalogs: List<FdkIdAndUri> = emptyList(),
    val changedResources: List<FdkIdAndUri> = emptyList()
)

data class FdkIdAndUri(
    val fdkId: String,
    val uri: String
)
