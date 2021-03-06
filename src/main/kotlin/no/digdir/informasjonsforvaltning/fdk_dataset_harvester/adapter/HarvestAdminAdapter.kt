package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

private val logger = LoggerFactory.getLogger(HarvestAdminAdapter::class.java)

@Service
class HarvestAdminAdapter(private val applicationProperties: ApplicationProperties) {

    fun urlWithParameters(params: Map<String, String>?): URL {
        val paramString: String = if (params != null && params.isNotEmpty()) {
            val paramList = mutableListOf<String>()
            params.forEach { paramList.add("${it.key}=${it.value}") }

            "?${paramList.joinToString("&")}"
        } else ""

        return URL("${applicationProperties.harvestAdminRootUrl}/datasources$paramString")
    }

    fun getDataSources(queryParams: Map<String, String>?): List<HarvestDataSource> {
        val url = urlWithParameters(queryParams)
        try {
            with(url.openConnection() as HttpURLConnection) {
                setRequestProperty("Accept", MediaType.APPLICATION_JSON.toString())
                setRequestProperty("Content-type", MediaType.APPLICATION_JSON.toString())

                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    val body = inputStream.bufferedReader().use(BufferedReader::readText)
                    return jacksonObjectMapper().readValue(body)
                } else {
                    logger.error("Fetch of harvest urls from $url failed, status: $responseCode", Exception("Fetch of data sources failed"))
                }
            }
        } catch (ex: Exception) {
            logger.error("Error fetching harvest urls from $url", ex)
        }
        return emptyList()
    }

}
