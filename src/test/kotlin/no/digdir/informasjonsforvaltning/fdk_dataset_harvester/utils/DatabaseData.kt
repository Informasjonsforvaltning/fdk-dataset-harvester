package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FDKCatalogTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FDKDatasetTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestSourceTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.TurtleDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.UNION_ID
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.gzip
import org.apache.jena.vocabulary.AS.partOf
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
    isPartOf = "http://localhost:5050/catalogs/6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f",
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
    isPartOf = "http://localhost:5050/catalogs/6f0a37af-a9c1-38bc-b343-bd025b43b5e8",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val REMOVED_DATASET_DBO = DatasetMeta(
    uri = "https://testdirektoratet.no/model/dataset/removed",
    fdkId = "removed",
    isPartOf = "http://localhost:5050/catalogs/6f0a37af-a9c1-38bc-b343-bd025b43b5e8",
    removed = true,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val UNION_DATA = FDKCatalogTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("all_catalogs.ttl"))
)

val UNION_DATA_NO_RECORDS = CatalogTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("all_catalogs_no_records.ttl"))
)

val HARVEST_DBO_0 = HarvestSourceTurtle(
    id = TEST_HARVEST_SOURCE_0.url!!,
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val HARVEST_DBO_1 = HarvestSourceTurtle(
    id = TEST_HARVEST_SOURCE_1.url!!,
    turtle = gzip(responseReader.readFile("harvest_response_1.ttl"))
)

val CATALOG_0_TURTLE = FDKCatalogTurtle(
    id = CATALOG_ID_0,
    turtle = gzip(responseReader.readFile("catalog_0.ttl"))
)

val CATALOG_0_TURTLE_NO_RECORDS = CatalogTurtle(
    id = CATALOG_ID_0,
    turtle = gzip(responseReader.readFile("catalog_0_no_records.ttl"))
)

val DATASET_0_TURTLE = FDKDatasetTurtle(
    id = DATASET_ID_0,
    turtle = gzip(responseReader.readFile("dataset_0.ttl"))
)

val DATASET_0_TURTLE_NO_RECORDS = DatasetTurtle(
    id = DATASET_ID_0,
    turtle = gzip(responseReader.readFile("parsed_dataset_0.ttl"))
)

val CATALOG_1_TURTLE = FDKCatalogTurtle(
    id = CATALOG_ID_1,
    turtle = gzip(responseReader.readFile("catalog_1.ttl"))
)

val CATALOG_1_TURTLE_NO_RECORDS = CatalogTurtle(
    id = CATALOG_ID_1,
    turtle = gzip(responseReader.readFile("catalog_1_no_records.ttl"))
)

val DATASET_1_TURTLE = FDKDatasetTurtle(
    id = DATASET_ID_1,
    turtle = gzip(responseReader.readFile("dataset_1.ttl"))
)

val DATASET_1_TURTLE_NO_RECORDS = DatasetTurtle(
    id = DATASET_ID_1,
    turtle = gzip(responseReader.readFile("parsed_dataset_1.ttl"))
)

val REMOVED_DATASET_TURTLE = FDKDatasetTurtle(
    id = "removed",
    turtle = gzip(responseReader.readFile("dataset_1.ttl"))
)

val REMOVED_DATASET_TURTLE_NO_RECORDS = DatasetTurtle(
    id = "removed",
    turtle = gzip(responseReader.readFile("parsed_dataset_1.ttl"))
)

fun catalogTurtlePopulation(): List<Document> =
    listOf(UNION_DATA_NO_RECORDS, CATALOG_0_TURTLE_NO_RECORDS, CATALOG_1_TURTLE_NO_RECORDS)
        .map { it.mapDBO() }

fun fdkCatalogTurtlePopulation(): List<Document> =
    listOf(UNION_DATA, CATALOG_0_TURTLE, CATALOG_1_TURTLE)
        .map { it.mapDBO() }

fun datasetTurtlePopulation(): List<Document> =
    listOf(DATASET_0_TURTLE_NO_RECORDS, DATASET_1_TURTLE_NO_RECORDS, REMOVED_DATASET_TURTLE_NO_RECORDS)
        .map { it.mapDBO() }

fun fdkDatasetTurtlePopulation(): List<Document> =
    listOf(DATASET_0_TURTLE, DATASET_1_TURTLE, REMOVED_DATASET_TURTLE)
        .map { it.mapDBO() }

fun sourceTurtlePopulation(): List<Document> =
    listOf(HARVEST_DBO_0, HARVEST_DBO_1)
        .map { it.mapDBO() }

fun catalogDBPopulation(): List<Document> =
    listOf(CATALOG_DBO_0, CATALOG_DBO_1)
        .map { it.mapDBO() }

fun datasetDBPopulation(): List<Document> =
    listOf(DATASET_DBO_0, DATASET_DBO_1, REMOVED_DATASET_DBO)
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
        .append("removed", removed)
        .append("issued", issued)
        .append("modified", modified)

private fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)