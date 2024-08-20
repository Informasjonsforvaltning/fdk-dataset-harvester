package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DuplicateIRI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.ApiTestContext
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_DBO_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_DBO_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.apiGet
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.authorizedRequest
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.jwk.Access
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.jwk.JwtToken
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class DatasetsContract : ApiTestContext() {
    private val responseReader = TestResponseReader()
    private val mapper = jacksonObjectMapper()

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
        val response = apiGet(port, "/datasets/$DATASET_ID_0?catalogrecords=true", "application/trig")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("dataset_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIG.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/datasets/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Nested
    internal inner class RemoveDatasetById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest(port, "/datasets/$DATASET_ID_0", null, HttpMethod.DELETE)
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/datasets/$DATASET_ID_0",
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundWhenIdNotInDB() {
            val response =
                authorizedRequest(port, "/datasets/123", JwtToken(Access.ROOT).toString(), HttpMethod.DELETE)
            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/datasets/$DATASET_ID_0",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])
        }
    }

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun unauthorizedForNoToken() {
            val body = listOf(DuplicateIRI(iriToRemove = DATASET_DBO_0.uri, iriToRetain = DATASET_DBO_1.uri))
            val response = authorizedRequest(
                port,
                "/datasets/duplicates",
                null,
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val body = listOf(DuplicateIRI(iriToRemove = DATASET_DBO_0.uri, iriToRetain = DATASET_DBO_1.uri))
            val response = authorizedRequest(
                port,
                "/datasets/duplicates",
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenRemoveIRINotInDB() {
            val body = listOf(DuplicateIRI(iriToRemove = "https://123.no", iriToRetain = DATASET_DBO_1.uri))
            val response =
                authorizedRequest(
                    port,
                    "/datasets/duplicates",
                    JwtToken(Access.ROOT).toString(),
                    HttpMethod.POST,
                    mapper.writeValueAsString(body)
                )
            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val body = listOf(DuplicateIRI(iriToRemove = DATASET_DBO_0.uri, iriToRetain = DATASET_DBO_1.uri))
            val response = authorizedRequest(
                port,
                "/datasets/duplicates",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.OK.value(), response["status"])
        }
    }

    @Nested
    internal inner class PurgeById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest(port, "/datasets/removed/purge", null, HttpMethod.DELETE)
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/datasets/removed/purge",
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenNotAlreadyRemoved() {
            val response = authorizedRequest(
                port,
                "/datasets/$DATASET_ID_1/purge",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun purgingStopsDeepLinking() {
            val pre = apiGet(port, "/datasets/removed", "text/turtle")
            assertEquals(HttpStatus.OK.value(), pre["status"])

            val response = authorizedRequest(
                port,
                "/datasets/removed/purge",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])

            val post = apiGet(port, "/datasets/removed", "text/turtle")
            assertEquals(HttpStatus.NOT_FOUND.value(), post["status"])
        }

    }

}
