package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.contract

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.ApiTestContext
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.apiGet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class DatasetsContract : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun getDatasetNoRecords() {
        val response = apiGet(port, "/datasets/$DATASET_ID_0", "application/rdf+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("parsed_dataset_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun getDatasetWithRecords() {
        val response = apiGet(port, "/datasets/$DATASET_ID_0?catalogrecords=true", "application/rdf+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("dataset_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/datasets/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

}