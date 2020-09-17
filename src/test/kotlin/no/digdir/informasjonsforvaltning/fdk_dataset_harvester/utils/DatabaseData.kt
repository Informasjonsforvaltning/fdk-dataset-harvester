package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.MiscellaneousTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.UNION_ID
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()

val CATALOG_DBO_0 = CatalogDBO(
    uri = "https://testdirektoratet.no/model/dataset-catalog/0",
    fdkId = CATALOG_ID_0,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("harvest_response_0.ttl")),
    turtleCatalog = gzip(responseReader.readFile("catalog_0.ttl"))
)

val DATASET_DBO_0 = DatasetDBO(
    uri = "https://testdirektoratet.no/model/dataset/0",
    fdkId = DATASET_ID_0,
    isPartOf = "http://localhost:5000/catalogs/6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("parsed_dataset_0.ttl")),
    turtleDataset = gzip(responseReader.readFile("dataset_0.ttl"))
)

val CATALOG_DBO_1 = CatalogDBO(
    uri = "https://testdirektoratet.no/model/dataset-catalog/1",
    fdkId = CATALOG_ID_1,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("harvest_response_1.ttl")),
    turtleCatalog = gzip(responseReader.readFile("catalog_1.ttl"))
)

val DATASET_DBO_1 = DatasetDBO(
    uri = "https://testdirektoratet.no/model/dataset/1",
    fdkId = DATASET_ID_1,
    isPartOf = "http://localhost:5000/catalogs/6f0a37af-a9c1-38bc-b343-bd025b43b5e8",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("parsed_dataset_1.ttl")),
    turtleDataset = gzip(responseReader.readFile("dataset_1.ttl"))
)


val UNION_DATA = MiscellaneousTurtle(
    id = UNION_ID,
    isHarvestedSource = false,
    turtle = gzip(responseReader.readFile("all_catalogs.ttl"))
)


val HARVEST_DBO_0 = MiscellaneousTurtle(
    id = TEST_HARVEST_SOURCE_0.url!!,
    isHarvestedSource = true,
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)


val HARVEST_DBO_1 = MiscellaneousTurtle(
    id = TEST_HARVEST_SOURCE_1.url!!,
    isHarvestedSource = true,
    turtle = gzip(responseReader.readFile("harvest_response_1.ttl"))
)

fun miscDBPopulation(): List<Document> =
    listOf(UNION_DATA, HARVEST_DBO_0, HARVEST_DBO_1)
        .map { it.mapDBO() }

fun catalogDBPopulation(): List<Document> =
    listOf(CATALOG_DBO_0, CATALOG_DBO_1)
        .map { it.mapDBO() }

fun datasetDBPopulation(): List<Document> =
    listOf(DATASET_DBO_0, DATASET_DBO_1)
        .map { it.mapDBO() }

private fun CatalogDBO.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("issued", issued)
        .append("modified", modified)
        .append("turtleHarvested", turtleHarvested)
        .append("turtleCatalog", turtleCatalog)

private fun DatasetDBO.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("isPartOf", isPartOf)
        .append("issued", issued)
        .append("modified", modified)
        .append("turtleHarvested", turtleHarvested)
        .append("turtleDataset", turtleDataset)

private fun MiscellaneousTurtle.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("isHarvestedSource", isHarvestedSource)
        .append("turtle", turtle)