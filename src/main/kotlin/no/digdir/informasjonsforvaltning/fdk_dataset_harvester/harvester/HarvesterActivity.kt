package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.HarvestAdminAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit.RabbitMQPublisher
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import java.util.Calendar

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val DATASET_TYPE = "dataset"
private const val HARVEST_ALL_ID = "all"

@Component
@Order(0)
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: DatasetHarvester,
    private val publisher: RabbitMQPublisher
): ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent?) {
        initiateHarvest(null)
    }

    fun initiateHarvest(params: MultiValueMap<String, String>?) {
        if (params == null) LOGGER.debug("starting harvest of all datasets")
        else LOGGER.debug("starting harvest with parameters $params")

        harvestAdminAdapter.getDataSources(params)
            .filter { it.dataType == DATASET_TYPE }
            .forEach {
                if (it.url != null) {
                    try {
                        harvester.harvestDatasetCatalog(it, Calendar.getInstance())
                    } catch (exception: Exception) {
                        LOGGER.error("Harvest of ${it.url} failed", exception)
                    }
                }
            }

        LOGGER.debug("completed harvest with parameters $params")

        publisher.send(HARVEST_ALL_ID)

    }
}