package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@Tag("unit")
class HarvestHelpersTest {

    private val responseReader = TestResponseReader()

    @Test
    fun extractedCatalogEqualsHarvestedCatalog() {

        val expected = responseReader.parseFile("harvest_response_3.ttl", "TURTLE")

        val catalog = extractCatalogs(expected, "test-url").first()

        assert(catalog.harvestedCatalog.isIsomorphicWith(expected))
    }

    @Test
    fun extractDatasetFromHarvestResponse() {

        val harvested = responseReader.parseFile("harvest_response_1.ttl", "TURTLE")

        val dataset = harvested
            .listResourcesWithProperty(RDF.type, DCAT.Dataset)
            .toList().first().extractDataset()

        val expected = responseReader.parseFile("parsed_dataset_1.ttl", "TURTLE")

        assert(dataset.harvestedDataset.isIsomorphicWith(expected))
    }

    @Test
    fun blankNodesAreSkolemized() {
        val harvested = responseReader.parseFile("catalog_with_blank_node.ttl", "TURTLE")

        val extracted = extractCatalogs(harvested, "https://example.com")
        assertEquals(1, extracted.size)

        val catalog = extracted.first()
        assertFalse(catalog.harvestedCatalog.isIsomorphicWith(harvested))

        val agents = catalog.harvestedCatalog.listResourcesWithProperty(RDF.type, FOAF.Agent).toList()
        assertEquals(1, agents.size)
        val blankAgent = agents.first()
        assertTrue(blankAgent.isURIResource)
        assertTrue(blankAgent.uri.contains("https://testdirektoratet.no/model/blank-catalog/0"))
        assertTrue(blankAgent.hasLiteral(DCTerms.identifier, "123456789"))
        assertTrue(blankAgent.hasLiteral(FOAF.name, "Blanke"))

        val periods = catalog.harvestedCatalog.listResourcesWithProperty(RDF.type, DCTerms.PeriodOfTime).toList()
        assertEquals(1, periods.size)
        val blankPeriod = periods.first()
        assertTrue(blankPeriod.isURIResource)
        assertTrue(blankPeriod.uri.contains("https://testdirektoratet.no/model/blank-dataset/0"))
        assertTrue(blankPeriod.hasLiteral(DCAT.startDate, "2019-04-02T00:00:00"))
        assertTrue(blankPeriod.hasLiteral(DCAT.endDate, "2022-06-06T00:00:00"))
    }

}