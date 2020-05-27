package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.HarvestFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.MetaFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_CATALOG_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_CATALOG_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_DATASET_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_DATASET_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.HARVEST_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.HARVEST_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class DatasetServiceTest {
    private val metaFuseki: MetaFuseki = mock()
    private val harvestFuseki: HarvestFuseki = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val datasetService = DatasetService(metaFuseki, harvestFuseki, applicationProperties)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class CountDatasetCatalogs {

        @Test
        fun handlesCountOfEmptyDB() {
            whenever(metaFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())

            val response = datasetService.countMetaData()

            assertEquals(0, response)
        }

        @Test
        fun correctCountFromUnionModel() {
            val allMetaData = responseReader.parseFile("all_metadata.ttl", "TURTLE")

            whenever(metaFuseki.fetchCompleteModel())
                .thenReturn(allMetaData)

            val response = datasetService.countMetaData()

            assertEquals(4, response)
        }

    }

    @Nested
    internal inner class AllCatalogs {

        @Test
        fun answerWithEmptyListWhenNoModelsSavedInFuseki() {
            whenever(metaFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())
            whenever(harvestFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())

            val expected = responseReader.parseResponse("", "TURTLE")

            val response = datasetService.getAll(JenaType.TURTLE)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

        @Test
        fun responseIsIsomorphicWithUnionOfModelsFromFuseki() {
            val allMetaData = responseReader.parseFile("all_metadata.ttl", "TURTLE")
            val dbHarvested0 = responseReader.parseResponse(HARVEST_0, "TURTLE")
            val dbHarvested1 = responseReader.parseResponse(HARVEST_1, "TURTLE")

            whenever(metaFuseki.fetchCompleteModel())
                .thenReturn(allMetaData)
            whenever(harvestFuseki.fetchCompleteModel())
                .thenReturn(dbHarvested0.union(dbHarvested1))

            val expected = allMetaData.union(dbHarvested0).union(dbHarvested1)

            val response = datasetService.getAll(JenaType.TURTLE)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

    }

    @Nested
    internal inner class CatalogById {

        @Test
        fun responseIsNullWhenNoMetaDataFound() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://host.testcontainers.internal:5000/catalogs/123>"))
                .thenReturn(null)

            whenever(applicationProperties.catalogUri)
                .thenReturn("http://host.testcontainers.internal:5000/catalogs")

            val response = datasetService.getDatasetCatalog("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://host.testcontainers.internal:5000/catalogs/$CATALOG_ID_0>"))
                .thenReturn(responseReader.parseResponse(META_CATALOG_0, "TURTLE"))

            whenever(harvestFuseki.fetchCompleteModel())
                .thenReturn(responseReader.parseResponse(HARVEST_0, "TURTLE"))

            whenever(applicationProperties.catalogUri)
                .thenReturn("http://host.testcontainers.internal:5000/catalogs")

            val response = datasetService.getDatasetCatalog(CATALOG_ID_0, JenaType.TURTLE)
            val expected = responseReader.parseFile("catalog_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

        @Test
        fun handlesMissingHarvestData() {
            val metaData = responseReader.parseResponse(META_CATALOG_0, "TURTLE")

            whenever(metaFuseki.queryDescribe("DESCRIBE <http://host.testcontainers.internal:5000/catalogs/$CATALOG_ID_0>"))
                .thenReturn(metaData)

            whenever(harvestFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())

            whenever(applicationProperties.catalogUri)
                .thenReturn("http://host.testcontainers.internal:5000/catalogs")

            val response = datasetService.getDatasetCatalog(CATALOG_ID_0, JenaType.TURTLE)

            assertTrue(metaData.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

    }

    @Nested
    internal inner class DatasetById {

        @Test
        fun responseIsNullWhenNoMetaDataFound() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://host.testcontainers.internal:5000/datasets/123>"))
                .thenReturn(null)
            whenever(applicationProperties.datasetUri)
                .thenReturn("http://host.testcontainers.internal:5000/datasets")

            val response = datasetService.getDataset("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://host.testcontainers.internal:5000/datasets/$DATASET_ID_0>"))
                .thenReturn(responseReader.parseResponse(META_DATASET_0, "TURTLE"))

            whenever(harvestFuseki.fetchCompleteModel())
                .thenReturn(responseReader.parseResponse(HARVEST_0, "TURTLE"))

            whenever(applicationProperties.datasetUri)
                .thenReturn("http://host.testcontainers.internal:5000/datasets")

            val response = datasetService.getDataset(DATASET_ID_0, JenaType.TURTLE)
            val expected = responseReader.parseFile("dataset_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

    }
}