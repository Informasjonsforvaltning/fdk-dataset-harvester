package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.MetaFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.HarvestFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.queryToGetMetaDataByUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.HARVEST_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_CATALOG_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_DATASET_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TEST_HARVEST_DATE
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TEST_HARVEST_SOURCE
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class HarvesterTest {
    private val harvestFuseki: HarvestFuseki = mock()
    private val metaFuseki: MetaFuseki = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: DatasetAdapter = mock()

    private val harvester = DatasetHarvester(adapter, metaFuseki, harvestFuseki, valuesMock)

    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSource() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE))
            .thenReturn(javaClass.classLoader.getResourceAsStream("harvest_response_0.ttl")!!.reader().readText())

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")


        val expectedSavedHarvest = responseReader.parseResponse(HARVEST_0, "TURTLE")
        val expectedCatalogMetaData = responseReader.parseResponse(META_CATALOG_0, "TURTLE")
        val expectedDatasetMetaData = responseReader.parseResponse(META_DATASET_0, "TURTLE")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<Model>().apply {
            verify(metaFuseki, times(2)).saveWithGraphName(any(), capture())
            assertTrue(firstValue.isIsomorphicWith(expectedCatalogMetaData))
            assertTrue(lastValue.isIsomorphicWith(expectedDatasetMetaData))
        }

        argumentCaptor<Model>().apply {
            verify(harvestFuseki, times(1)).saveWithGraphName(any(), capture())
            assertTrue(firstValue.isIsomorphicWith(expectedSavedHarvest))
        }

    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE))
            .thenReturn(javaClass.classLoader.getResourceAsStream("harvest_response_error.ttl")!!.reader().readText())

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<Model>().apply {
            verify(metaFuseki, times(0)).saveWithGraphName(any(), capture())
        }

        argumentCaptor<Model>().apply {
            verify(harvestFuseki, times(0)).saveWithGraphName(any(), capture())
        }
    }

}