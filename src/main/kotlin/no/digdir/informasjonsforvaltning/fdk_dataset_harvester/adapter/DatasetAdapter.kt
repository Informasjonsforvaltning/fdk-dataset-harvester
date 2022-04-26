package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester.HarvestException
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

private val LOGGER = LoggerFactory.getLogger(DatasetAdapter::class.java)
private const val TEN_MINUTES = 600000

@Service
class DatasetAdapter {

    fun getDatasets(source: HarvestDataSource): String {
        val connection = URL(source.url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", source.acceptHeaderValue)
        connection.connectTimeout = TEN_MINUTES
        connection.readTimeout = TEN_MINUTES

        if (connection.responseCode != HttpStatus.OK.value()) {
            val exception = HarvestException("Harvest failed for ${source.url}, status was ${connection.responseCode}")
            throw exception
        } else {
            return connection
                .inputStream
                .bufferedReader()
                .use(BufferedReader::readText)
        }
    }

}
