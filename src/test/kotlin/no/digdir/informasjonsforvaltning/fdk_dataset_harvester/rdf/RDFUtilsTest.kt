package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.URL
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

        val parsedRDFModel = parseRDFResponse(rdfBody, JenaType.TURTLE)

        val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

        Assertions.assertTrue(parsedRDFModel.isIsomorphicWith(expected))
    }

    @Test
    fun createDatasetModel() {
        val harvestedModel0 = responseReader.parseFile("harvest_response.ttl", "TURTLE")
        val expected0 = responseReader.parseFile("harvested_dataset.ttl", "TURTLE")
        val harvestedModel1 = responseReader.parseFile("harvest_response_1.ttl", "TURTLE")
        val expected1 = responseReader.parseFile("harvested_dataset_1.ttl", "TURTLE")

        val datasetModel0 = harvestedModel0
            .listResourcesWithProperty(RDF.type, DCAT.Dataset)
            .toList().first().createDatasetModel()

        val datasetModel1 = harvestedModel1
            .listResourcesWithProperty(RDF.type, DCAT.Dataset)
            .toList().first().createDatasetModel()

        Assertions.assertTrue(datasetModel0.isIsomorphicWith(expected0))
        Assertions.assertTrue(datasetModel1.isIsomorphicWith(expected1))
    }
}