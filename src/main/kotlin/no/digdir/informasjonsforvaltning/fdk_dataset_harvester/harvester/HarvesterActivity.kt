package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.HarvestAdminAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit.RabbitMQPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import java.util.*
import javax.annotation.PostConstruct

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val DATASET_TYPE = "dataset"
private const val HARVEST_ALL_ID = "all"

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: DatasetHarvester,
    private val publisher: RabbitMQPublisher
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)
    private val harvestSemaphore = Semaphore(2)

    @PostConstruct
    private fun fullHarvestOnStartup() = initiateHarvest(null)

    fun initiateHarvest(params: MultiValueMap<String, String>?) {
        if (params == null) LOGGER.debug("starting harvest of all datasets")
        else LOGGER.debug("starting harvest with parameters $params")

        val harvest = launch {
            activitySemaphore.withPermit {
                harvestAdminAdapter.getDataSources(params)
                    .filter { it.dataType == DATASET_TYPE }
                    .forEach {
                        if (it.url != null) {
                            launch {
                                harvestSemaphore.withPermit {
                                    try {
                                        harvester.harvestDatasetCatalog(it, Calendar.getInstance())
                                    } catch (exception: Exception) {
                                        LOGGER.error("Harvest of ${it.url} failed", exception)
                                    }
                                }
                            }
                        }
                    }
            }
        }

        val onHarvestCompletion = launch {
            harvest.join()
            harvester.updateUnionModel()

            if (params != null) LOGGER.debug("completed harvest with parameters $params")
            else LOGGER.debug("completed full harvest")

            publisher.sendUpdateAssessmentsMessage()

            harvest.cancelChildren()
            harvest.cancel()

        }

        onHarvestCompletion.invokeOnCompletion { onHarvestCompletion.cancel() }
    }
}