package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test


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

}