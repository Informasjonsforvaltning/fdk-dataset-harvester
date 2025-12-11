package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestReport
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestTrigger
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit.RabbitMQPublisher
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Calendar
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)

@Service
class HarvesterActivity(
    private val harvester: DatasetHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)

    fun initiateHarvest(trigger: HarvestTrigger) {
        LOGGER.debug("starting harvest of datasource ${trigger.dataSourceId} (${trigger.dataSourceUrl}), force update: ${trigger.forceUpdate}")

        launch {
            try {
                activitySemaphore.withPermit {
                    val harvestDate = Calendar.getInstance()
                    val (report, timeElapsed) = measureTimedValue {
                        harvester.harvestDatasetCatalog(trigger, harvestDate)
                    }
                    
                    Metrics.counter("harvest_count",
                            "status", if (report?.harvestError == false) { "success" }  else { "error" },
                            "type", "dataset",
                            "force_update", "${trigger.forceUpdate}",
                            "datasource_id", trigger.dataSourceId ?: "unknown",
                            "datasource_url", trigger.dataSourceUrl ?: "unknown"
                    ).increment()
                    
                    if (report?.harvestError == false) {
                        Metrics.counter("harvest_changed_resources_count",
                                "type", "dataset",
                                "force_update", "${trigger.forceUpdate}",
                                "datasource_id", trigger.dataSourceId ?: "unknown",
                                "datasource_url", trigger.dataSourceUrl ?: "unknown"
                        ).increment(report.changedResources.size.toDouble())
                        Metrics.counter("harvest_removed_resources_count",
                                "type", "dataset",
                                "force_update", "${trigger.forceUpdate}",
                                "datasource_id", trigger.dataSourceId ?: "unknown",
                                "datasource_url", trigger.dataSourceUrl ?: "unknown"
                        ).increment(report.removedResources.size.toDouble())
                        Metrics.timer("harvest_time",
                                "type", "dataset",
                                "force_update", "${trigger.forceUpdate}",
                                "datasource_id", trigger.dataSourceId ?: "unknown",
                                "datasource_url", trigger.dataSourceUrl ?: "unknown").record(timeElapsed.toJavaDuration())
                    }
                    
                    report?.let { 
                        logHarvestReport(it, timeElapsed)
                        updateService.updateMetaData()
                        sendRabbitMessages(listOf(it))
                        LOGGER.debug("completed harvest of datasource ${trigger.dataSourceId}, forced update: ${trigger.forceUpdate}")
                    }
                }
            } catch(ex: Exception) {
                LOGGER.error("harvest failure for datasource ${trigger.dataSourceId}", ex)
            }
        }
    }

    private fun logHarvestReport(report: HarvestReport, timeElapsed: kotlin.time.Duration) {
        if (report.harvestError) {
            LOGGER.warn("Harvest report indicates error: runId=${report.runId}, dataSourceId=${report.dataSourceId}, dataSourceUrl=${report.dataSourceUrl}, errorMessage=${report.errorMessage}")
        } else {
            LOGGER.info("Harvest report summary: runId=${report.runId}, dataSourceId=${report.dataSourceId}, dataSourceUrl=${report.dataSourceUrl}, " +
                    "startTime=${report.startTime}, endTime=${report.endTime}, duration=${timeElapsed.inWholeSeconds}s, " +
                    "changedCatalogs=${report.changedCatalogs.size}, changedResources=${report.changedResources.size}, removedResources=${report.removedResources.size}")
            
            if (report.changedCatalogs.isNotEmpty()) {
                LOGGER.debug("Changed catalogs: ${report.changedCatalogs.joinToString(", ") { "${it.fdkId} (${it.uri})" }}")
            }
            if (report.changedResources.isNotEmpty()) {
                LOGGER.debug("Changed resources: ${report.changedResources.take(10).joinToString(", ") { "${it.fdkId} (${it.uri})" }}${if (report.changedResources.size > 10) " ... and ${report.changedResources.size - 10} more" else ""}")
            }
            if (report.removedResources.isNotEmpty()) {
                LOGGER.debug("Removed resources: ${report.removedResources.take(10).joinToString(", ") { "${it.fdkId} (${it.uri})" }}${if (report.removedResources.size > 10) " ... and ${report.removedResources.size - 10} more" else ""}")
            }
        }
    }

    private fun sendRabbitMessages(reports: List<HarvestReport>) {
        LOGGER.debug("Sending ${reports.size} harvest report(s) to RabbitMQ")
        reports.forEachIndexed { index, report ->
            LOGGER.debug("Sending harvest report ${index + 1}/${reports.size}: runId=${report.runId}, dataSourceId=${report.dataSourceId}, " +
                    "changedCatalogs=${report.changedCatalogs.size}, changedResources=${report.changedResources.size}, removedResources=${report.removedResources.size}")
        }
        publisher.send(reports)
        LOGGER.info("Successfully sent ${reports.size} harvest report(s) to RabbitMQ")
    }
}
