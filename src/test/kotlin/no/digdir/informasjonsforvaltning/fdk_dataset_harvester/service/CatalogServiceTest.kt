package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.CATALOG_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class CatalogServiceTest {
    private val catalogFuseki: CatalogFuseki = mock()
    private val catalogService: CatalogService = CatalogService(catalogFuseki)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class CountDataServiceCatalogs {

        @Test
        fun handlesCountOfEmptyDB() {
            whenever(catalogFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())

            val response = catalogService.countDatasetCatalogs()

            assertEquals(0, response)
        }

        @Test
        fun correctCountFromUnionModel() {
            val dbCatalog0 = responseReader.parseFile("db_catalog_0.json", "JSONLD")
            val dbCatalog1 = responseReader.parseFile("db_catalog_1.json", "JSONLD")

            whenever(catalogFuseki.fetchCompleteModel())
                .thenReturn(dbCatalog0.union(dbCatalog1))

            val response = catalogService.countDatasetCatalogs()

            assertEquals(2, response)
        }

    }

    @Nested
    internal inner class AllCatalogs {

        @Test
        fun answerWithEmptyListWhenNoModelsSavedInFuseki() {
            whenever(catalogFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())

            val expected = responseReader.parseResponse("", "TURTLE")

            val response = catalogService.getAllDatasetCatalogs(JenaType.TURTLE)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

        @Test
        fun responseIsIsomorphicWithUnionOfModelsFromFuseki() {
            val dbCatalog0 = responseReader.parseFile("db_catalog_0.json", "JSONLD")
            val dbCatalog1 = responseReader.parseFile("db_catalog_1.json", "JSONLD")

            whenever(catalogFuseki.fetchCompleteModel())
                .thenReturn(dbCatalog0.union(dbCatalog1))

            val expected = dbCatalog0.union(dbCatalog1)

            val response = catalogService.getAllDatasetCatalogs(JenaType.TURTLE)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

    }

    @Nested
    internal inner class CatalogById {

        @Test
        fun responseIsNullWhenNotFoundInFuseki() {
            whenever(catalogFuseki.fetchByGraphName("123"))
                .thenReturn(null)

            val response = catalogService.getDatasetCatalog("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithModelFromFuseki() {
            val dbCatalog = responseReader.parseFile("db_catalog_0.json", "JSONLD")

            whenever(catalogFuseki.fetchByGraphName(CATALOG_ID_0))
                .thenReturn(dbCatalog)

            val response = catalogService.getDatasetCatalog(CATALOG_ID_0, JenaType.TURTLE)

            assertTrue(dbCatalog.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

    }
}