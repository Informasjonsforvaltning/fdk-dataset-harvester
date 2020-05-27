package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.dto.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.ApiTestContainer.Companion.TEST_API
import java.util.Calendar
import java.util.TimeZone

const val API_PORT = 8080
const val LOCAL_SERVER_PORT = 5000

const val FUSEKI_NETWORK_NAME = "fdk-fuseki-service"

const val WIREMOCK_TEST_HOST = "host.testcontainers.internal"
const val WIREMOCK_TEST_URI = "http://$WIREMOCK_TEST_HOST:$LOCAL_SERVER_PORT"

val API_ENV_VALUES: Map<String, String> = mapOf(
    "SPRING_PROFILES_ACTIVE" to "contract-test",
    "WIREMOCK_TEST_HOST" to WIREMOCK_TEST_HOST,
    "WIREMOCK_TEST_URI" to WIREMOCK_TEST_URI,
    "WIREMOCK_TEST_PORT" to LOCAL_SERVER_PORT.toString()
)

const val DATASET_ID_0 = "a1c680ca-62d7-34d5-aa4c-d39b5db033ae"
const val DATASET_ID_1 = "4667277a-9d27-32c1-aed5-612fa601f393"
const val CATALOG_ID_0 = "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f"
const val CATALOG_ID_1 = "6f0a37af-a9c1-38bc-b343-bd025b43b5e8"

fun getApiAddress(endpoint: String): String {
    return "http://${TEST_API.containerIpAddress}:${TEST_API.getMappedPort(API_PORT)}$endpoint"
}

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 2, 12).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/harvest0",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val ERROR_HARVEST_SOURCE = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/error-harvest",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val TEST_HARVEST_SOURCE_ID_0 = createIdFromUri("$WIREMOCK_TEST_URI/harvest0")
val TEST_HARVEST_SOURCE_ID_1 = createIdFromUri("$WIREMOCK_TEST_URI/harvest1")
