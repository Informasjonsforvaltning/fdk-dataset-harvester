package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

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
import org.springframework.stereotype.Service
import java.util.Calendar
import javax.annotation.PostConstruct

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

    @PostConstruct
    private fun fullHarvestOnStartup() = initiateHarvest(HarvestAdminParameters(null, null, null), false)

    fun initiateHarvest(params: HarvestAdminParameters, forceUpdate: Boolean) {
        if (params.harvestAllDatasets()) LOGGER.debug("starting harvest of all datasets, force update: $forceUpdate")
        else LOGGER.debug("starting harvest with parameters $params, force update: $forceUpdate")

        launch {
            try {
                activitySemaphore.withPermit {
                    harvestAdminAdapter.getDataSources(params)
                        .filter { it.dataType == DATASET_TYPE }
                        .filter { it.url != null }
                        .map { async { harvester.harvestDatasetCatalog(it, Calendar.getInstance(), forceUpdate) } }
                        .awaitAll()
                        .filterNotNull()
                        .also { updateService.updateMetaData() }
                        .also {
                            if (params.harvestAllDatasets()) LOGGER.debug("completed harvest with parameters $params, forced update: $forceUpdate")
                            else LOGGER.debug("completed full harvest, forced update: $forceUpdate")
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
