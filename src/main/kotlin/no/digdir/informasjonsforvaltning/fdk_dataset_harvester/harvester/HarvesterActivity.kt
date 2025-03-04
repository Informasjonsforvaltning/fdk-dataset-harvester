package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.HarvestAdminAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestAdminParameters
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestReport
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit.RabbitMQPublisher
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Calendar
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val DATASET_TYPE = "dataset"

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: DatasetHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)

    @EventListener
    fun fullHarvestOnStartup(event: ApplicationReadyEvent) =
        initiateHarvest(HarvestAdminParameters(null, null, null), false)

    @Scheduled(cron = "0 0 * * * *")
    fun scheduledHarvest() =
        initiateHarvest(HarvestAdminParameters(null, null, null), false)

    fun initiateHarvest(params: HarvestAdminParameters, forceUpdate: Boolean) {
        if (params.harvestAllDatasets()) LOGGER.debug("starting harvest of all datasets, force update: $forceUpdate")
        else LOGGER.debug("starting harvest with parameters $params, force update: $forceUpdate")

        launch {
            try {
                activitySemaphore.withPermit {
                    harvestAdminAdapter.getDataSources(params)
                        .filter { it.dataType == DATASET_TYPE }
                        .filter { it.url != null }
                        .map { async {
                            val harvestDate = Calendar.getInstance()
                            val (report, timeElapsed) = measureTimedValue {
                                harvester.harvestDatasetCatalog(it, harvestDate, forceUpdate)
                            }
                            Metrics.counter("harvest_count",
                                    "status", if (report?.harvestError == false) { "success" }  else { "error" },
                                    "type", "dataset",
                                    "force_update", "$forceUpdate",
                                    "datasource_id", it.id,
                                    "datasource_url", it.url
                            ).increment()
                            if (report?.harvestError == false) {
                                Metrics.counter("harvest_changed_resources_count",
                                        "type", "dataset",
                                        "force_update", "$forceUpdate",
                                        "datasource_id", it.id,
                                        "datasource_url", it.url
                                ).increment(report.changedResources.size.toDouble())
                                Metrics.counter("harvest_removed_resources_count",
                                        "type", "dataset",
                                        "force_update", "$forceUpdate",
                                        "datasource_id", it.id,
                                        "datasource_url", it.url
                                ).increment(report.removedResources.size.toDouble())
                                Metrics.timer("harvest_time",
                                        "type", "dataset",
                                        "force_update", "$forceUpdate",
                                        "datasource_id", it.id,
                                        "datasource_url", it.url).record(timeElapsed.toJavaDuration())
                            }
                            report
                        } }
                        .awaitAll()
                        .filterNotNull()
                        .also { updateService.updateMetaData() }
                        .also {
                            LOGGER.debug("completed harvest with parameters $params, forced update: $forceUpdate")
                        }
                        .run { sendRabbitMessages() }
                }
            } catch(ex: Exception) {
                LOGGER.error("harvest failure", ex)
            }
        }
    }

    private fun List<HarvestReport>.sendRabbitMessages() {
        publisher.send(this)
        LOGGER.debug("Successfully sent harvest completed message")
    }
}
