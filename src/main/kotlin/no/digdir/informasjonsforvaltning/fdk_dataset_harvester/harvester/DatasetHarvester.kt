package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.dto.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(DatasetHarvester::class.java)

@Service
class DatasetHarvester {

    fun harvestDataServiceCatalog(source: HarvestDataSource, harvestDate: Calendar) {
        LOGGER.info("harvest ${source.url}")
    }
}