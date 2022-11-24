package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Tag("unit")
class UpdateServiceTest {
    private val catalogRepository: CatalogRepository = mock()
    private val datasetRepository: DatasetRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val turtleService: TurtleService = mock()
    private val updateService = UpdateService(valuesMock, catalogRepository, datasetRepository, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class UpdateMetaData {

        @Test
        fun catalogRecordsIsRecreatedFromMetaDBO() {
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0))
            whenever(datasetRepository.findAll())
                .thenReturn(listOf(DATASET_DBO_0, DATASET_DBO_1))
            whenever(datasetRepository.findAllByIsPartOf("http://localhost:5000/catalogs/$CATALOG_ID_0"))
                .thenReturn(listOf(DATASET_DBO_0, DATASET_DBO_1))
            whenever(turtleService.getCatalog(CATALOG_ID_0, false))
                .thenReturn(responseReader.readFile("catalog_0_no_records.ttl"))
            whenever(turtleService.getDataset(DATASET_ID_0, false))
                .thenReturn(responseReader.readFile("parsed_dataset_0.ttl"))
            whenever(turtleService.getDataset(DATASET_ID_1, false))
                .thenReturn(responseReader.readFile("parsed_dataset_1.ttl"))

            whenever(valuesMock.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")
            whenever(valuesMock.datasetUri)
                .thenReturn("http://localhost:5000/datasets")

            updateService.updateMetaData()

            val expectedCatalog = responseReader.parseFile("catalog_0.ttl", "TURTLE")
            val expectedDataset0 = responseReader.parseFile("dataset_0.ttl", "TURTLE")
            val expectedDataset1 = responseReader.parseFile("dataset_1.ttl", "TURTLE")

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(2)).saveAsDataset(first.capture(), second.capture(), third.capture())
                assertTrue(first.firstValue.isIsomorphicWith(expectedDataset0))
                assertTrue(first.secondValue.isIsomorphicWith(expectedDataset1))
                assertEquals(listOf(DATASET_ID_0, DATASET_ID_1), second.allValues)
                assertEquals(listOf(true, true), third.allValues)
            }

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(1)).saveAsCatalog(first.capture(), second.capture(), third.capture())
                assertTrue(first.firstValue.isIsomorphicWith(expectedCatalog))
                assertEquals(CATALOG_ID_0, second.firstValue)
                assertEquals(listOf(true), third.allValues)
            }
        }

        @Test
        fun catalogRecordsIsCreatedForDatasetSeries() {
            val catalogMeta = CatalogMeta(
                uri = "http://example.org/EUCatalog",
                fdkId = "catalog-id",
                issued = TEST_HARVEST_DATE.timeInMillis,
                modified = TEST_HARVEST_DATE.timeInMillis
            )
            val datasetMeta = DatasetMeta(
                uri = "http://example.org/budget",
                fdkId = "dataset-series-id",
                isPartOf = "http://localhost:5000/catalogs/catalog-id",
                issued = TEST_HARVEST_DATE.timeInMillis,
                modified = TEST_HARVEST_DATE.timeInMillis
            )
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(catalogMeta))
            whenever(datasetRepository.findAll())
                .thenReturn(listOf(datasetMeta))
            whenever(datasetRepository.findAllByIsPartOf("http://localhost:5000/catalogs/${catalogMeta.fdkId}"))
                .thenReturn(listOf(datasetMeta))
            whenever(turtleService.getCatalog(catalogMeta.fdkId, false))
                .thenReturn(responseReader.readFile("parsed_catalog_with_series.ttl"))
            whenever(turtleService.getDataset(datasetMeta.fdkId, false))
                .thenReturn(responseReader.readFile("parsed_dataset_series.ttl"))

            whenever(valuesMock.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")
            whenever(valuesMock.datasetUri)
                .thenReturn("http://localhost:5000/datasets")

            updateService.updateMetaData()

            val expectedDataset = responseReader.parseFile("dataset_series.ttl", "TURTLE")
            val expectedCatalog = responseReader.parseFile("catalog_with_series.ttl", "TURTLE")

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(1)).saveAsDataset(first.capture(), second.capture(), third.capture())
                assertTrue(first.firstValue.isIsomorphicWith(expectedDataset))
                assertEquals(listOf(datasetMeta.fdkId), second.allValues)
                assertEquals(listOf(true), third.allValues)
            }

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(1)).saveAsCatalog(first.capture(), second.capture(), third.capture())
                assertTrue(first.firstValue.isIsomorphicWith(expectedCatalog))
                assertEquals(catalogMeta.fdkId, second.firstValue)
                assertEquals(listOf(true), third.allValues)
            }
        }

    }

    @Nested
    internal inner class UpdateUnionModel {

        @Test
        fun updateUnionModel() {
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0, CATALOG_DBO_1))

            whenever(turtleService.getCatalog(CATALOG_ID_0, true))
                .thenReturn(responseReader.readFile("catalog_0.ttl"))
            whenever(turtleService.getCatalog(CATALOG_ID_1, true))
                .thenReturn(responseReader.readFile("catalog_1.ttl"))

            whenever(turtleService.getCatalog(CATALOG_ID_0, false))
                .thenReturn(responseReader.readFile("catalog_0_no_records.ttl"))
            whenever(turtleService.getCatalog(CATALOG_ID_1, false))
                .thenReturn(responseReader.readFile("catalog_1_no_records.ttl"))

            updateService.updateUnionModels()

            val catalogUnion = responseReader.parseFile("all_catalogs.ttl", "TURTLE")
            val noRecordsUnion = responseReader.parseFile("all_catalogs_no_records.ttl", "TURTLE")

            argumentCaptor<Model, Boolean>().apply {
                verify(turtleService, times(2)).saveAsCatalogUnion(first.capture(), second.capture())
                assertTrue(first.firstValue.isIsomorphicWith(catalogUnion))
                assertTrue(first.secondValue.isIsomorphicWith(noRecordsUnion))
                assertEquals(listOf(true, false), second.allValues)
            }
        }
    }
}