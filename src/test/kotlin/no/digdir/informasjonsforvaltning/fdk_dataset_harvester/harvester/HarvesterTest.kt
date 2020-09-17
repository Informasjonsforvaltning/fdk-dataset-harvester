package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.FusekiAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.MiscellaneousTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.MiscellaneousRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.gzip
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

@Tag("unit")
class HarvesterTest {
    private val catalogRepository: CatalogRepository = mock()
    private val datasetRepository: DatasetRepository = mock()
    private val miscellaneousRepository: MiscellaneousRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val fusekiAdapter: FusekiAdapter = mock()
    private val adapter: DatasetAdapter = mock()

    private val harvester = DatasetHarvester(adapter, catalogRepository,
        datasetRepository, miscellaneousRepository, valuesMock, fusekiAdapter)

    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscellaneousRepository, times(1)).save(capture())
            assertEquals(HARVEST_DBO_0, firstValue)
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            assertEquals(CATALOG_DBO_0, firstValue.first())
        }

        argumentCaptor<List<DatasetDBO>>().apply {
            verify(datasetRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            assertEquals(DATASET_DBO_0, firstValue.first())
        }

    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

        whenever(miscellaneousRepository.findById("http://localhost:5000/harvest0"))
            .thenReturn(Optional.of(HARVEST_DBO_0.copy(turtle = gzip(responseReader.readFile("harvest_response_0.ttl")))))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscellaneousRepository, times(0)).save(capture())
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(0)).saveAll(capture())
        }

        argumentCaptor<List<DatasetDBO>>().apply {
            verify(datasetRepository, times(0)).saveAll(capture())
        }

    }

    @Test
    fun onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

        val catalogDiffTurtle = gzip(responseReader.readFile("harvest_response_0_catalog_diff.ttl"))

        whenever(miscellaneousRepository.findById("http://localhost:5000/harvest0"))
            .thenReturn(Optional.of(HARVEST_DBO_0.copy(turtle = catalogDiffTurtle)))

        whenever(catalogRepository.findById("https://testdirektoratet.no/model/dataset-catalog/0"))
            .thenReturn(Optional.of(CATALOG_DBO_0.copy(turtleHarvested = catalogDiffTurtle)))

        whenever(datasetRepository.findById("https://testdirektoratet.no/model/dataset/0"))
            .thenReturn(Optional.of(DATASET_DBO_0))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        val expectedCatalogDBO = CATALOG_DBO_0.copy(
            modified = NEW_TEST_HARVEST_DATE.timeInMillis,
            turtleCatalog = gzip(responseReader.readFile("catalog_0_diff_update.ttl"))
        )

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, NEW_TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscellaneousRepository, times(1)).save(capture())
            assertEquals(HARVEST_DBO_0, firstValue)
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            assertEquals(expectedCatalogDBO, firstValue.first())
        }

        argumentCaptor<List<DatasetDBO>>().apply {
            verify(datasetRepository, times(1)).saveAll(capture())
            assertEquals(0, firstValue.size)
        }

    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_error.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscellaneousRepository, times(0)).save(capture())
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(0)).saveAll(capture())
        }

        argumentCaptor<List<DatasetDBO>>().apply {
            verify(datasetRepository, times(0)).saveAll(capture())
        }
    }

}