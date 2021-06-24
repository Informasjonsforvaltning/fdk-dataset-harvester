package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

private val LOGGER = LoggerFactory.getLogger(DatasetAdapter::class.java)

@Service
class DatasetAdapter {

    fun getDatasets(source: HarvestDataSource): String? =
        try {
            val connection = URL(source.url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", source.acceptHeaderValue)

            if (connection.responseCode != HttpStatus.OK.value()) {
                LOGGER.error(Exception("Harvest from ${source.url} has failed").stackTraceToString())
                null
            } else {
                connection
                    .inputStream
                    .bufferedReader()
                    .use(BufferedReader::readText)
            }

        } catch (ex: Exception) {
            LOGGER.error("${ex.stackTraceToString()}: Error when harvesting from ${source.url}")
            null
        }

}
