package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.dto.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.jenaTypeFromAcceptHeader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.parseRDFResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(DatasetHarvester::class.java)

@Service
class DatasetHarvester(private val adapter: DatasetAdapter) {

    fun harvestDataServiceCatalog(source: HarvestDataSource, harvestDate: Calendar) {
        LOGGER.debug("Starting harvest of ${source.url}")
        val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

        if (jenaWriterType == null || jenaWriterType == JenaType.NOT_ACCEPTABLE) {
            LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable ")
        } else {
            adapter.getDataServiceCatalog(source)
                ?.let { parseRDFResponse(it, jenaWriterType) }
        }
    }
}