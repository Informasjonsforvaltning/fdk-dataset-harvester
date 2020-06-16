package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.HarvestFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.MetaFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.MissingHarvestException
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_CATALOG_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.META_DATASET_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.HARVEST_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.HARVEST_1
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://localhost:5000/catalogs/123>"))
                .thenReturn(null)

            whenever(applicationProperties.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")

            val response = datasetService.getDatasetCatalog("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://localhost:5000/catalogs/$CATALOG_ID_0>"))
                .thenReturn(responseReader.parseResponse(META_CATALOG_0, "TURTLE"))

            whenever(harvestFuseki.queryDescribe("DESCRIBE <https://testdirektoratet.no/model/dataset-catalog/0>"))
                .thenReturn(responseReader.parseFile("catalog_0_no_uri_properties.ttl", "TURTLE"))

            whenever(harvestFuseki.queryDescribe("DESCRIBE * WHERE { <https://testdirektoratet.no/model/dataset-catalog/0> ?p ?o }"))
                .thenReturn(responseReader.parseFile("dataset_0_no_uri_properties.ttl", "TURTLE"))

            whenever(harvestFuseki.queryDescribe("PREFIX dcat: <http://www.w3.org/ns/dcat#> PREFIX dct: <http://purl.org/dc/terms/> PREFIX dcatapi: <http://dcat.no/dcatapi/> DESCRIBE * WHERE { <https://testdirektoratet.no/model/dataset-catalog/0> dcat:dataset/dcat:distribution|dcat:dataset/dcat:distribution/dcatapi:accessService|dcat:dataset/dct:publisher|dcat:dataset/dcat:contactPoint|dcat:dataset/dct:spatial ?o }"))
                .thenReturn(responseReader.parseFile("distribution_0_no_uri_properties.ttl", "TURTLE").union(responseReader.parseFile("distribution_0_uri_properties.ttl", "TURTLE")))

            whenever(applicationProperties.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")

            val response = datasetService.getDatasetCatalog(CATALOG_ID_0, JenaType.TURTLE)
            val expected = responseReader.parseFile("catalog_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

        @Test
        fun throwExceptionWhenHarvestDataMissing() {
            val metaData = responseReader.parseResponse(META_CATALOG_0, "TURTLE")

            whenever(metaFuseki.queryDescribe("DESCRIBE <http://localhost:5000/catalogs/$CATALOG_ID_0>"))
                .thenReturn(metaData)

            whenever(applicationProperties.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")

            assertThrows<MissingHarvestException> { datasetService.getDatasetCatalog(CATALOG_ID_0, JenaType.TURTLE) }
        }

    }

    @Nested
    internal inner class DatasetById {

        @Test
        fun responseIsNullWhenNoMetaDataFound() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://localhost:5000/datasets/123>"))
                .thenReturn(null)
            whenever(applicationProperties.datasetUri)
                .thenReturn("http://localhost:5000/datasets")

            val response = datasetService.getDataset("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(metaFuseki.queryDescribe("DESCRIBE <http://localhost:5000/datasets/$DATASET_ID_0>"))
                .thenReturn(responseReader.parseResponse(META_DATASET_0, "TURTLE"))

            whenever(harvestFuseki.queryDescribe("DESCRIBE <https://testdirektoratet.no/model/dataset/0>"))
                .thenReturn(responseReader.parseFile("dataset_0_no_uri_properties.ttl", "TURTLE"))

            whenever(harvestFuseki.queryDescribe("DESCRIBE * WHERE { <https://testdirektoratet.no/model/dataset/0> ?p ?o }"))
                .thenReturn(responseReader.parseFile("distribution_0_no_uri_properties.ttl", "TURTLE"))

            whenever(harvestFuseki.queryDescribe("PREFIX dcat: <http://www.w3.org/ns/dcat#> PREFIX dcatapi: <http://dcat.no/dcatapi/> DESCRIBE * WHERE { <https://testdirektoratet.no/model/dataset/0> dcat:distribution/dcatapi:accessService ?o }"))
                .thenReturn(responseReader.parseFile("distribution_0_uri_properties.ttl", "TURTLE"))

            whenever(applicationProperties.datasetUri)
                .thenReturn("http://localhost:5000/datasets")

            val response = datasetService.getDataset(DATASET_ID_0, JenaType.TURTLE)
            val expected = responseReader.parseFile("dataset_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

    }
}