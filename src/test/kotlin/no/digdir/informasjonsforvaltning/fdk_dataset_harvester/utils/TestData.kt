package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestDataSource
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import java.util.Calendar
import java.util.TimeZone

const val API_PORT = 8080
const val API_TEST_PORT = 5555
const val LOCAL_SERVER_PORT = 5000

const val API_TEST_URI = "http://localhost:$API_TEST_PORT"
const val WIREMOCK_TEST_URI = "http://localhost:$LOCAL_SERVER_PORT"

const val DATASET_ID_0 = "a1c680ca-62d7-34d5-aa4c-d39b5db033ae"
const val DATASET_ID_1 = "4667277a-9d27-32c1-aed5-612fa601f393"
const val CATALOG_ID_0 = "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f"
const val CATALOG_ID_1 = "6f0a37af-a9c1-38bc-b343-bd025b43b5e8"
const val SPARQL_PREFIX_DCT = "PREFIX dct: <http://purl.org/dc/terms/>"
const val SPARQL_PREFIX_DCAT = "PREFIX dcat: <http://www.w3.org/ns/dcat#"
const val SPARQL_UPDATE_QUERY = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
        "INSERT DATA\n" +
        "{ \n" +
        "  <http://example/book1> dc:title \"A new book\" ;\n" +
        "                         dc:creator \"A.N.Other\" .\n" +
        "}"
const val SPARQL_ASK_ENDPOINT= "/sparql/ask"
const val SPARQL_SELECT_ENDPOINT= "/sparql/select"

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 2, 12).setTimeOfDay(11, 52, 16, 122).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 6, 12).setTimeOfDay(11, 52, 16, 122).build()

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
