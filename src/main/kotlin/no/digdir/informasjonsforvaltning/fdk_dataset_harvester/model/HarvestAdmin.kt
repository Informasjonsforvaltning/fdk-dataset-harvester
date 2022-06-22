package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestDataSource (
    val id: String? = null,
    val dataSourceType: String? = null,
    val dataType: String? = null,
    val url: String? = null,
    val acceptHeaderValue: String? = null
)

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestAdminParameters(
    val dataSourceId: String?,
    val publisherId: String?,
    val dataSourceType: String?,
    val dataType: String? = "dataset"
) {
    fun harvestAllDatasets(): Boolean =
        dataSourceId == null && publisherId == null && dataSourceType == null
}
