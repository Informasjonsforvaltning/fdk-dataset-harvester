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
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.queryToGetMetaDataByUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.HARVEST_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_CATALOG_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_DATASET_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.NEW_TEST_HARVEST_DATE
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
    fun harvestDataSourceSavedWhenDBIsEmpty() {
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
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE))
            .thenReturn(javaClass.classLoader.getResourceAsStream("harvest_response_0.ttl")!!.reader().readText())

        whenever(harvestFuseki.fetchByGraphName(createIdFromUri("http://localhost:5000/harvest0")))
            .thenReturn(responseReader.parseFile("harvest_response_0.ttl", "TURTLE"))

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

    @Test
    fun onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE))
            .thenReturn(javaClass.classLoader.getResourceAsStream("harvest_response_0.ttl")!!.reader().readText())

        whenever(harvestFuseki.fetchByGraphName(createIdFromUri("http://localhost:5000/harvest0")))
            .thenReturn(responseReader.parseFile("harvest_response_0_catalog_diff.ttl", "TURTLE"))

        whenever(metaFuseki.queryDescribe(queryToGetMetaDataByUri("https://testdirektoratet.no/model/dataset-catalog/0")))
            .thenReturn(responseReader.parseResponse(META_CATALOG_0, "TURTLE"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        val expectedCatalogMetaData = responseReader.parseFile("harvest_response_0_catalog_meta.ttl", "TURTLE")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE)

        argumentCaptor<Model>().apply {
            verify(metaFuseki, times(1)).saveWithGraphName(any(), capture())
            assertTrue(firstValue.isIsomorphicWith(expectedCatalogMetaData))
        }

        argumentCaptor<Model>().apply {
            verify(harvestFuseki, times(1)).saveWithGraphName(any(), capture())
        }

    }

    @Test
    fun onlyRelevantMetaUpdatedWhenOneDatasetChangedFromDB() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE))
            .thenReturn(javaClass.classLoader.getResourceAsStream("harvest_response_2.ttl")!!.reader().readText())

        whenever(harvestFuseki.fetchByGraphName(createIdFromUri("http://localhost:5000/harvest0")))
            .thenReturn(responseReader.parseFile("harvest_response_2_dataset_diff.ttl", "TURTLE"))

        whenever(metaFuseki.queryDescribe(queryToGetMetaDataByUri("https://testdirektoratet.no/model/dataset-catalog/0")))
            .thenReturn(responseReader.parseResponse(META_CATALOG_0, "TURTLE"))
        whenever(metaFuseki.queryDescribe(queryToGetMetaDataByUri("https://testdirektoratet.no/model/dataset/0")))
            .thenReturn(responseReader.parseResponse(META_DATASET_0, "TURTLE"))

        val expectedCatalogMetaData = responseReader.parseFile("harvest_response_2_catalog_meta.ttl", "TURTLE")
        val expectedDatasetMetaData = responseReader.parseFile("harvest_response_2_dataset_meta.ttl", "TURTLE")

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE)

        argumentCaptor<Model>().apply {
            verify(metaFuseki, times(2)).saveWithGraphName(any(), capture())
            assertTrue(firstValue.isIsomorphicWith(expectedCatalogMetaData))
            assertTrue(lastValue.isIsomorphicWith(expectedDatasetMetaData))
        }

        argumentCaptor<Model>().apply {
            verify(harvestFuseki, times(1)).saveWithGraphName(any(), capture())
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