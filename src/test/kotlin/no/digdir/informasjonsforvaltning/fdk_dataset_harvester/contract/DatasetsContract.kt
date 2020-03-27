package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.contract

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.ApiTestContainer
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.apiGet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpStatus
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")
class DataSetsContract : ApiTestContainer() {
    private val responseReader = TestResponseReader()

    @Test
    fun findSpecific() {
        val response = apiGet("/datasets/$DATASET_ID_0", "application/rdf+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("dataset_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet("/datasets/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findAll() {
        val response = apiGet("/datasets", "application/ld+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("all_datasets.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "JSONLD")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }
}