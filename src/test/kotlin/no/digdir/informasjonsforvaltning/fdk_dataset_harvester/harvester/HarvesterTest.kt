package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TEST_HARVEST_DATE
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TEST_HARVEST_SOURCE
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class HarvesterTest {
    private val datasetFuseki: DatasetFuseki = mock()
    private val catalogFuseki: CatalogFuseki = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: DatasetAdapter = mock()

    private val harvester = DatasetHarvester(adapter, catalogFuseki, datasetFuseki, valuesMock)

    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSource() {
        whenever(adapter.getDatasetCatalog(TEST_HARVEST_SOURCE))
            .thenReturn(javaClass.classLoader.getResourceAsStream("harvest_response_0.ttl")!!.reader().readText())

        whenever(valuesMock.catalogUri)
            .thenReturn("https://datasets.fellesdatakatalog.digdir.no/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("https://datasets.fellesdatakatalog.digdir.no/datasets")

        val expectedCatalog = responseReader.parseFile("db_catalog_0.json", "JSONLD")
        val expectedDataset = responseReader.parseFile("db_dataset_0.json", "JSONLD")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<Model>().apply {
            verify(catalogFuseki, times(1)).saveWithGraphName(any(), capture())
            assertTrue(firstValue.isIsomorphicWith(expectedCatalog))
        }

        argumentCaptor<Model>().apply {
            verify(datasetFuseki, times(1)).saveWithGraphName(any(), capture())
            assertTrue(firstValue.isIsomorphicWith(expectedDataset))
        }

    }

}