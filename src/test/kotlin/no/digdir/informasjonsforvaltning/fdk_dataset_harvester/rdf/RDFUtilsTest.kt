package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Tag("unit")
class RDFUtilsTest {
    private val responseReader = TestResponseReader()

    @Test
    fun createId() {
        assertEquals(DATASET_ID_0, createIdFromUri("https://testdirektoratet.no/model/dataset/0"))
        assertEquals(DATASET_ID_1, createIdFromUri("https://testdirektoratet.no/model/dataset/1"))
        assertEquals(CATALOG_ID_0, createIdFromUri("https://testdirektoratet.no/model/dataset-catalog/0"))
        assertEquals(CATALOG_ID_1, createIdFromUri("https://testdirektoratet.no/model/dataset-catalog/1"))
    }

    @Test
    fun rdfModelParser() {
        val rdfBody: String = javaClass.classLoader.getResourceAsStream("all_catalogs.ttl")!!.reader().readText()

        val parsedRDFModel = parseRDFResponse(rdfBody, Lang.TURTLE, "test")

        val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

        Assertions.assertTrue(parsedRDFModel!!.isIsomorphicWith(expected))
    }
}