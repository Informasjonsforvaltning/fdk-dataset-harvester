package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.contract

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.junit.jupiter.api.Assertions
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
class SparqlSelectContract : ApiTestContext() {

    @Test
    fun testOrganizationWithDatasetsQueryStringResponse() {
        val organizationWithDatasetsQuery : String = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * \n" +
                "{ ?dataset a dcat:Dataset }"
        val result = apiGet(
                endpoint = "${SPARQL_SELECT_ENDPOINT}?query=${organizationWithDatasetsQuery.encodeForSparql()}",
                acceptHeader = "text/plain")
        Assertions.assertEquals(200, result["status"])

    }
    @Test
    fun testOrganizationsWithDatasetsQueryJsonResponse() {
        val organizationWithDatasetsQuery: String = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * \n" +
                "{ ?dataset a dcat:Dataset }"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${organizationWithDatasetsQuery.encodeForSparql()}",
                acceptHeader = "application/json")
        Assertions.assertEquals(200, result["status"])
        Assertions.assertTrue(isJson(result["body"] as String?))
    }
    @Test
    fun testOrganizationsWithDatasetsQueryXMLResponse() {
        val organizationWithDatasetsQuery : String  = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * \n" +
                "{ ?dataset a dcat:Dataset }"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${organizationWithDatasetsQuery.encodeForSparql()}",
                acceptHeader = "application/xml")
        Assertions.assertEquals(200, result["status"])
        Assertions.assertTrue(isXml(result["body"] as String?))
    }
    @Test
    fun testDatasetsFromPublisherQuery() {
        val datasetsFromPublisherQuery :String = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "SELECT *\n" +
                "{\n" +
                "?dataset a dcat:Catalog ;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER(?publisher=<https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${datasetsFromPublisherQuery.encodeForSparql()}",
                acceptHeader = "text/plain")
        Assertions.assertEquals(200, result["status"])
    }
    @Test
    fun testDatasetsFromPublisherQueryJsonResponse() {
        val datasetsFromPublisherQuery : String = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "SELECT *\n" +
                "{\n" +
                "?dataset a dcat:Catalog ;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER(?publisher=<https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${datasetsFromPublisherQuery.encodeForSparql()}",
                acceptHeader = "application/json")
        Assertions.assertEquals(200, result["status"])
        Assertions.assertTrue(isJson(result["body"] as String?))
    }
    @Test
    fun testDatasetsFromPublisherQueryXmlResponse() {
        val datasetsFromPublisherQuery : String = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "SELECT *\n" +
                "{\n" +
                "?dataset a dcat:Catalog ;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER(?publisher=<https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${datasetsFromPublisherQuery.encodeForSparql()}",
                acceptHeader = "application/xml")
        Assertions.assertEquals(200, result["status"])
        Assertions.assertTrue(isXml(result["body"] as String?))
    }
    @Test
    fun testShouldReturn204forQueriesWithNoResult() {
        val datasetsFromPublisherQuery : String = "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * {\n" +
                "?dataset a dcat:Dataset;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER(?publisher=<https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${datasetsFromPublisherQuery.encodeForSparql()}",
                acceptHeader = "application/json")
        Assertions.assertEquals(204, result["status"])
    }
    @Test
    fun testShouldReturn204forQueriesWithNoResultJsonResponse() {
        val datasetsFromPublisherQuery : String = "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * {\n" +
                "?dataset a dcat:Dataset;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER(?publisher=<https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${datasetsFromPublisherQuery.encodeForSparql()}",
                acceptHeader = "application/json")
        Assertions.assertEquals(204, result["status"])
    }
    @Test
    fun testShouldReturn204forQueriesWithNoResultXmlResponse() {
        val datasetsFromPublisherQuery : String = "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * {\n" +
                "?dataset a dcat:Dataset;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER(?publisher=<https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${datasetsFromPublisherQuery.encodeForSparql()}",
                acceptHeader = "application/xml")
        Assertions.assertEquals(204, result["status"])
    }
    @Test
    fun testShouldReturn400forMalformedSparqlQueries() {
        val malformedSparqlQuery : String = "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "SELECT * {\n" +
                "?dataset a dcat:Dataset;\n" +
                "dct:publisher ?publisher\n" +
                "FILTER?publisher=(<https://register.geonorge.no/organisasjoner/kartverket/10087020-f17c-45e1-8542-02acbcf3d8a777666aa>)\n" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${malformedSparqlQuery.encodeForSparql()}",
                acceptHeader = "application/json")
        Assertions.assertEquals(400, result["status"])
    }
    @Test
    fun testShouldReturn400forQueriesContainingSparqlUpdate() {
        val sparqlUpdateQuery : String = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "PREFIX ns: <http://example.org/ns#>\n" +
                "INSERT DATA\n" +
                "{" +
                "GRAPH <http://example/bookStore> { <http://example/book1>  ns:price  42 }" +
                "}"
        val result = apiGet(
                endpoint = "$SPARQL_SELECT_ENDPOINT?query=${sparqlUpdateQuery.encodeForSparql()}",
                acceptHeader = "application/json")
        Assertions.assertEquals(400, result["status"])
    }
}
