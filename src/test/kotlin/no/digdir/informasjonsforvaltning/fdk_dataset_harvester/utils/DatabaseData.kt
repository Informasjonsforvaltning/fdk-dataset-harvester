package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.TurtleDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.UNION_ID
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.catalogTurtleID
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.datasetTurtleID
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()

val CATALOG_DBO_0 = CatalogMeta(
    uri = "https://testdirektoratet.no/model/dataset-catalog/0",
    fdkId = CATALOG_ID_0,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val DATASET_DBO_0 = DatasetMeta(
    uri = "https://testdirektoratet.no/model/dataset/0",
    fdkId = DATASET_ID_0,
    isPartOf = "http://localhost:5000/catalogs/6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val CATALOG_DBO_1 = CatalogMeta(
    uri = "https://testdirektoratet.no/model/dataset-catalog/1",
    fdkId = CATALOG_ID_1,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val DATASET_DBO_1 = DatasetMeta(
    uri = "https://testdirektoratet.no/model/dataset/1",
    fdkId = DATASET_ID_1,
    isPartOf = "http://localhost:5000/catalogs/6f0a37af-a9c1-38bc-b343-bd025b43b5e8",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val UNION_DATA = TurtleDBO(
    id = catalogTurtleID(UNION_ID, true),
    turtle = gzip(responseReader.readFile("all_catalogs.ttl"))
)

val UNION_DATA_NO_RECORDS = TurtleDBO(
    id = catalogTurtleID(UNION_ID, false),
    turtle = gzip(responseReader.readFile("all_catalogs_no_records.ttl"))
)

val HARVEST_DBO_0 = TurtleDBO(
    id = TEST_HARVEST_SOURCE_0.url!!,
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val HARVEST_DBO_1 = TurtleDBO(
    id = TEST_HARVEST_SOURCE_1.url!!,
    turtle = gzip(responseReader.readFile("harvest_response_1.ttl"))
)

val CATALOG_0_TURTLE = TurtleDBO(
    id = catalogTurtleID(CATALOG_ID_0, true),
    turtle = gzip(responseReader.readFile("catalog_0.ttl"))
)

val CATALOG_0_TURTLE_NO_RECORDS = TurtleDBO(
    id = catalogTurtleID(CATALOG_ID_0, false),
    turtle = gzip(responseReader.readFile("catalog_0_no_records.ttl"))
)

val DATASET_0_TURTLE = TurtleDBO(
    id = datasetTurtleID(DATASET_ID_0, true),
    turtle = gzip(responseReader.readFile("dataset_0.ttl"))
)

val DATASET_0_TURTLE_NO_RECORDS = TurtleDBO(
    id = datasetTurtleID(DATASET_ID_0, false),
    turtle = gzip(responseReader.readFile("parsed_dataset_0.ttl"))
)

val CATALOG_1_TURTLE = TurtleDBO(
    id = catalogTurtleID(CATALOG_ID_1, true),
    turtle = gzip(responseReader.readFile("catalog_1.ttl"))
)

val CATALOG_1_TURTLE_NO_RECORDS = TurtleDBO(
    id = catalogTurtleID(CATALOG_ID_1, false),
    turtle = gzip(responseReader.readFile("catalog_1_no_records.ttl"))
)

val DATASET_1_TURTLE = TurtleDBO(
    id = datasetTurtleID(DATASET_ID_1, true),
    turtle = gzip(responseReader.readFile("dataset_1.ttl"))
)

val DATASET_1_TURTLE_NO_RECORDS = TurtleDBO(
    id = datasetTurtleID(DATASET_ID_1, false),
    turtle = gzip(responseReader.readFile("parsed_dataset_1.ttl"))
)

fun turtleDBPopulation(): List<Document> =
    listOf(
        UNION_DATA,
        UNION_DATA_NO_RECORDS,
        HARVEST_DBO_0,
        HARVEST_DBO_1,
        CATALOG_0_TURTLE,
        CATALOG_0_TURTLE_NO_RECORDS,
        DATASET_0_TURTLE,
        DATASET_0_TURTLE_NO_RECORDS,
        CATALOG_1_TURTLE,
        CATALOG_1_TURTLE_NO_RECORDS,
        DATASET_1_TURTLE,
        DATASET_1_TURTLE_NO_RECORDS
    )
        .map { it.mapDBO() }

fun catalogDBPopulation(): List<Document> =
    listOf(CATALOG_DBO_0, CATALOG_DBO_1)
        .map { it.mapDBO() }

fun datasetDBPopulation(): List<Document> =
    listOf(DATASET_DBO_0, DATASET_DBO_1)
        .map { it.mapDBO() }

private fun CatalogMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("issued", issued)
        .append("modified", modified)

private fun DatasetMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("isPartOf", isPartOf)
        .append("issued", issued)
        .append("modified", modified)

private fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)