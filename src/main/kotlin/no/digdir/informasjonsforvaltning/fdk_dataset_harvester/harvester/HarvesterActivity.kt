package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.HarvestAdminAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import java.util.*
import javax.annotation.PostConstruct

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val DATASET_TYPE = "dataset"

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: DatasetHarvester
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    @PostConstruct
    private fun fullHarvestOnStartup() = initiateHarvest(null)

    fun initiateHarvest(params: MultiValueMap<String, String>?) {
        LOGGER.debug("starting harvest with parameters $params")

        val harvest = launch {
            harvestAdminAdapter.getDataSources(params)
                .filter { it.dataType == DATASET_TYPE }
                .forEach {
                    if (it.url != null) {
                        launch { harvester.harvestDataServiceCatalog(it, Calendar.getInstance()) }
                    }
                }
        }

        launch {
            harvest.join()
            LOGGER.debug("completed harvest with parameters $params")
        }
    }
}