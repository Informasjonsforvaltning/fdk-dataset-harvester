package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.irix.IRIException
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@Tag("unit")
class RDFUtilsTest {
    private val responseReader = TestResponseReader()

    @Test
    fun createId() {
        assertEquals(DATASET_ID_0, createIdFromString("https://testdirektoratet.no/model/dataset/0"))
        assertEquals(DATASET_ID_1, createIdFromString("https://testdirektoratet.no/model/dataset/1"))
        assertEquals(CATALOG_ID_0, createIdFromString("https://testdirektoratet.no/model/dataset-catalog/0"))
        assertEquals(CATALOG_ID_1, createIdFromString("https://testdirektoratet.no/model/dataset-catalog/1"))
    }

    @Test
    fun rdfModelParser() {
        val rdfBody: String = javaClass.classLoader.getResourceAsStream("all_catalogs.ttl")!!.reader().readText()

        val parsedRDFModel = parseRDFResponse(rdfBody, Lang.TURTLE)

        val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

        Assertions.assertTrue(parsedRDFModel!!.isIsomorphicWith(expected))
    }

    @Test
    fun parseFailsWhenInvalidAsXML() {
        val invalidAsXML = """
            @prefix dcat:     <http://www.w3.org/ns/dcat#> .
            @prefix rdf:      <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            <https://data.test.no/.well-known/skolem/cb6fc14f-6e47-34bd-819c-1e105630b8ec>
                rdf:type             dcat:Distribution ;
                dcat:accessURL       <ftp://test.com/~Test/vegnett/ruteplan_esri> .
        """.trimIndent()
        assertThrows<IRIException> {
            parseRDFResponse(invalidAsXML, Lang.TURTLE)
        }
    }
}
