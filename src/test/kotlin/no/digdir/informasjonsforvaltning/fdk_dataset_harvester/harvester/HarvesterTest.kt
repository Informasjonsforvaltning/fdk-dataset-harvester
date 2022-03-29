package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import com.nhaarman.mockitokotlin2.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FdkIdAndUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestReport
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.TurtleService
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

@Tag("unit")
class HarvesterTest {
    private val catalogRepository: CatalogRepository = mock()
    private val datasetRepository: DatasetRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val turtleService: TurtleService = mock()
    private val adapter: DatasetAdapter = mock()

    private val harvester = DatasetHarvester(adapter, catalogRepository,
        datasetRepository, turtleService, valuesMock)

    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(datasetRepository.findById(DATASET_ID_0))
            .thenReturn(Optional.of(DATASET_DBO_0))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        val report = harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            Assertions.assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            Assertions.assertEquals(TEST_HARVEST_SOURCE_0.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(1)).saveAsCatalog(first.capture(), second.capture(), third.capture())
            Assertions.assertTrue(first.allValues[0].isIsomorphicWith(responseReader.parseFile("catalog_0_no_records.ttl", "TURTLE")))
            assertEquals(listOf(CATALOG_ID_0), second.allValues)
            Assertions.assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(CATALOG_DBO_0, firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(1)).saveAsDataset(first.capture(), second.capture(), third.capture())
            Assertions.assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("parsed_dataset_0.ttl", "TURTLE")))
            assertEquals(DATASET_ID_0, second.firstValue)
            assertEquals(false, third.firstValue)
        }

        argumentCaptor<DatasetMeta>().apply {
            verify(datasetRepository, times(1)).save(capture())
            assertEquals(DATASET_DBO_0, firstValue)
        }

        val expectedReport = HarvestReport(
            id="harvest0",
            url="http://localhost:5000/harvest0",
            dataType="dataset",
            harvestError=false,
            timestamp=TEST_HARVEST_DATE.timeInMillis,
            errorMessage=null,
            changedCatalogs=listOf(FdkIdAndUri(fdkId="6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f", uri="https://testdirektoratet.no/model/dataset-catalog/0"))
        )

        assertEquals(expectedReport, report)
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(harvested)
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(harvested)

        val report = harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(0)).saveAsHarvestSource(first.capture(), second.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsCatalog(first.capture(), second.capture(), third.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsDataset(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(0)).save(capture())
        }
        argumentCaptor<DatasetMeta>().apply {
            verify(datasetRepository, times(0)).save(capture())
        }

        val expectedReport = HarvestReport(
            id="harvest0",
            url="http://localhost:5000/harvest0",
            dataType="dataset",
            harvestError=false,
            timestamp=TEST_HARVEST_DATE.timeInMillis,
            errorMessage=null
        )

        assertEquals(expectedReport, report)
    }

    @Test
    fun onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB() {
     whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
         .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
     whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
         .thenReturn(responseReader.readFile("harvest_response_0_catalog_diff.ttl"))

     whenever(valuesMock.catalogUri)
         .thenReturn("http://localhost:5000/catalogs")
     whenever(valuesMock.datasetUri)
         .thenReturn("http://localhost:5000/datasets")

     whenever(catalogRepository.findById(CATALOG_DBO_0.uri))
         .thenReturn(Optional.of(CATALOG_DBO_0))
     whenever(datasetRepository.findById(DATASET_DBO_0.uri))
         .thenReturn(Optional.of(DATASET_DBO_0))

     whenever(turtleService.getCatalog(CATALOG_ID_0, false))
         .thenReturn(responseReader.readFile("harvest_response_0_catalog_diff.ttl"))
     whenever(turtleService.getDataset(DATASET_ID_0, false))
         .thenReturn(responseReader.readFile("parsed_dataset_0.ttl"))

     val report = harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, NEW_TEST_HARVEST_DATE)

     argumentCaptor<Model, String>().apply {
         verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
         Assertions.assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
         Assertions.assertEquals(TEST_HARVEST_SOURCE_0.url, second.firstValue)
     }

     argumentCaptor<CatalogMeta>().apply {
         verify(catalogRepository, times(1)).save(capture())
         assertEquals(CATALOG_DBO_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis), firstValue)
     }

     argumentCaptor<DatasetMeta>().apply {
         verify(datasetRepository, times(0)).save(capture())
     }

     argumentCaptor<Model, String, Boolean>().apply {
         verify(turtleService, times(1)).saveAsCatalog(first.capture(), second.capture(), third.capture())
         Assertions.assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("catalog_0_no_records.ttl", "TURTLE")))
         assertEquals(listOf(CATALOG_ID_0), second.allValues)
         Assertions.assertEquals(listOf(false), third.allValues)
     }

     argumentCaptor<Model, String, Boolean>().apply {
         verify(turtleService, times(0)).saveAsDataset(first.capture(), second.capture(), third.capture())
     }

        val expectedReport = HarvestReport(
            id="harvest0",
            url="http://localhost:5000/harvest0",
            dataType="dataset",
            harvestError=false,
            timestamp=NEW_TEST_HARVEST_DATE.timeInMillis,
            errorMessage=null,
            changedCatalogs=listOf(FdkIdAndUri(fdkId="6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f", uri="https://testdirektoratet.no/model/dataset-catalog/0"))
        )

        assertEquals(expectedReport, report)
    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getDatasets(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_error.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.datasetUri)
            .thenReturn("http://localhost:5000/datasets")

        val report = harvester.harvestDatasetCatalog(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(0)).saveAsHarvestSource(first.capture(), second.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsCatalog(first.capture(), second.capture(), third.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsDataset(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(0)).save(capture())
        }
        argumentCaptor<DatasetMeta>().apply {
            verify(datasetRepository, times(0)).save(capture())
        }

        val expectedReport = HarvestReport(
            id="harvest0",
            url="http://localhost:5000/harvest0",
            dataType="dataset",
            harvestError=true,
            timestamp=TEST_HARVEST_DATE.timeInMillis,
            errorMessage="[line: 6, col: 86] Bad character in IRI (space): <https://testdirektoratet.no/whitespace/in-iri/err[space]...>"
        )

        assertEquals(expectedReport, report)
    }

}