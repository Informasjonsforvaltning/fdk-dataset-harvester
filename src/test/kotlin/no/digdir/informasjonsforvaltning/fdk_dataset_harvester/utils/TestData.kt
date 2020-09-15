package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.MiscellaneousTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.gzip
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.Calendar
import java.util.TimeZone

private val responseReader = TestResponseReader()

const val API_PORT = 8080
const val API_TEST_PORT = 5555
const val LOCAL_SERVER_PORT = 5000

const val API_TEST_URI = "http://localhost:$API_TEST_PORT"
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

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 2, 12).setTimeOfDay(11, 52, 16, 122).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 6, 12).setTimeOfDay(11, 52, 16, 122).build()

val CATALOG_DBO_0 = CatalogDBO(
    uri = "https://testdirektoratet.no/model/dataset-catalog/0",
    fdkId = CATALOG_ID_0,
    issued = TEST_HARVEST_DATE,
    modified = listOf(TEST_HARVEST_DATE),
    turtleHarvested = gzip(responseReader.readFile("harvest_response_0.ttl")),
    turtleCatalog = gzip(responseReader.readFile("catalog_0.ttl"))
)
val DATASET_DBO_0 = DatasetDBO(
    uri = "https://testdirektoratet.no/model/dataset/0",
    fdkId = DATASET_ID_0,
    isPartOf = "http://localhost:5000/catalogs/6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f",
    issued = TEST_HARVEST_DATE,
    modified = listOf(TEST_HARVEST_DATE),
    turtleHarvested = gzip(responseReader.readFile("parsed_dataset_0.ttl")),
    turtleDataset = gzip(responseReader.readFile("dataset_0.ttl"))
)

val TEST_HARVEST_SOURCE_0 = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/harvest0",
    acceptHeaderValue = "text/turtle",
    dataType = "dataset",
    dataSourceType = "DCAT-AP-NO"
)

val TEST_HARVEST_SOURCE_1 = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/harvest1",
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

val SAVED_HARVEST = MiscellaneousTurtle(
    id = TEST_HARVEST_SOURCE_0.url!!,
    isHarvestedSource = true,
    turtle = gzip(HARVEST_0)
)

val TEST_HARVEST_SOURCE_ID_0 = createIdFromUri("$WIREMOCK_TEST_URI/harvest0")
val TEST_HARVEST_SOURCE_ID_1 = createIdFromUri("$WIREMOCK_TEST_URI/harvest1")
