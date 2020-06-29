package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.contract

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        properties = ["spring.profiles.active=contract-test"],
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
@Disabled
class SparqlAskContract : ApiTestContext() {

    @Test
    fun testShouldReturn200forTrueResult() {
        val askOrganizationHasMoreThan10Datasets =
                "$SPARQL_PREFIX_DCAT\n" +
                "$SPARQL_PREFIX_DCT\n" +
                "ASK\n" +
                        "{\n" +
                        "  ?entry a dcat:Dataset;\n" +
                        "         dct:publisher ?publisher \n" +
                        "} \n" +
                        "HAVING(count(?entry) > 10)"
        val result = apiGet(
                endpoint = "$SPARQL_ASK_ENDPOINT?query=$askOrganizationHasMoreThan10Datasets",
                acceptHeader = null)
        assertEquals(200,result["status"])
    }
    @Test
    fun testShouldReturn204forFalse() {
        val askOrganizationHasMoreThan10000Datasets =
                "$SPARQL_PREFIX_DCAT\n" +
                        "$SPARQL_PREFIX_DCT\n" +
                        "ASK\n" +
                        "{\n" +
                        "  ?entry a dcat:Dataset;\n" +
                        "         dct:publisher ?publisher \n" +
                        "} \n" +
                        "HAVING(count(?entry) > 10000)"
        val result = apiGet(
                endpoint = "$SPARQL_ASK_ENDPOINT?query=$askOrganizationHasMoreThan10000Datasets",
                acceptHeader = null)
        assertEquals(204,result["status"])
    }
    @Test
    fun testShouldReturn400forMalformedSparqlQueries() {
        val malformedAskQuery =
                "$SPARQL_PREFIX_DCAT\n" +
                        "$SPARQL_PREFIX_DCT\n" +
                        "ASK\n ?entry" +
                        "{\n" +
                        "  ?entry a dcat:Dataset;\n" +
                        "         dct:publisher ?publisher \n" +
                        "} \n" +
                        "HAVING(count(?entry) > 10000)"
        val result = apiGet(
                endpoint = "$SPARQL_ASK_ENDPOINT?query=$malformedAskQuery",
                acceptHeader = null)
        assertEquals(204,result["status"])
        
    }
    @Test
    fun testShouldReturn400forQueriesContainingUpdate() {}
    @Test
    fun testShouldReturn400forQueriesContainingInsert() {
    }

}