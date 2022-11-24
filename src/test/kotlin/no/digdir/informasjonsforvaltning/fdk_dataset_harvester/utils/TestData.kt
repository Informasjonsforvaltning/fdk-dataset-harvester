package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromString
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.Calendar
import java.util.TimeZone

const val LOCAL_SERVER_PORT = 5000
const val WIREMOCK_TEST_URI = "http://localhost:$LOCAL_SERVER_PORT"

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

const val DATASET_ID_0 = "a1c680ca-62d7-34d5-aa4c-d39b5db033ae"
const val DATASET_ID_1 = "4667277a-9d27-32c1-aed5-612fa601f393"
const val CATALOG_ID_0 = "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f"
const val CATALOG_ID_1 = "6f0a37af-a9c1-38bc-b343-bd025b43b5e8"
const val CATALOG_ID_4 = "df68b420-fb97-3770-9580-7518734632b1"

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 2, 12).setTimeOfDay(11, 52, 16, 122).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 6, 12).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE_0 = HarvestDataSource(
    id = "harvest0",
    url = "$WIREMOCK_TEST_URI/harvest0",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val TEST_HARVEST_SOURCE_1 = HarvestDataSource(
    id = "harvest1",
    url = "$WIREMOCK_TEST_URI/harvest1",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val TEST_HARVEST_SOURCE_4 = HarvestDataSource(
    id = "harvest4",
    url = "$WIREMOCK_TEST_URI/harvest4",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val ERROR_HARVEST_SOURCE = HarvestDataSource(
    id = "error-harvest",
    url = "$WIREMOCK_TEST_URI/error-harvest",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val TEST_HARVEST_SOURCE_ID_0 = createIdFromString("$WIREMOCK_TEST_URI/harvest0")
val TEST_HARVEST_SOURCE_ID_1 = createIdFromString("$WIREMOCK_TEST_URI/harvest1")
