package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.MiscellaneousRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class DatasetServiceTest {
    private val catalogRepository: CatalogRepository = mock()
    private val datasetRepository: DatasetRepository = mock()
    private val miscRepository: MiscellaneousRepository = mock()
    private val datasetService = DatasetService(catalogRepository, datasetRepository, miscRepository)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllCatalogs {

        @Test
        fun responseIsometricWithEmptyModelForEmptyDB() {
            whenever(miscRepository.findById(UNION_ID))
                .thenReturn(Optional.empty())

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = datasetService.getAll(JenaType.TURTLE)
            val responseJsonLD = datasetService.getAll(JenaType.JSON_LD)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            val allCatalogs = MiscellaneousTurtle(
                id = UNION_ID,
                isHarvestedSource = false,
                turtle = javaClass.classLoader.getResource("all_catalogs.ttl")!!.readText()
            )

            whenever(miscRepository.findById(UNION_ID))
                .thenReturn(Optional.of(allCatalogs))

            val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

            val responseTurtle = datasetService.getAll(JenaType.TURTLE)
            val responseN3 = datasetService.getAll(JenaType.N3)
            val responseNTriples = datasetService.getAll(JenaType.NTRIPLES)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class CatalogById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(catalogRepository.findOneByFdkId("123"))
                .thenReturn(null)

            val response = datasetService.getDatasetCatalog("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(catalogRepository.findOneByFdkId(CATALOG_ID_0))
                .thenReturn(CATALOG_DBO_0)

            val responseTurtle = datasetService.getDatasetCatalog(CATALOG_ID_0, JenaType.TURTLE)
            val responseJsonRDF = datasetService.getDatasetCatalog(CATALOG_ID_0, JenaType.RDF_JSON)

            val expected = responseReader.parseFile("catalog_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonRDF!!, "RDF/JSON")))
        }

    }

    @Nested
    internal inner class DatasetById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(datasetRepository.findOneByFdkId("123"))
                .thenReturn(null)

            val response = datasetService.getDataset("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(datasetRepository.findOneByFdkId(DATASET_ID_0))
                .thenReturn(DATASET_DBO_0)

            val responseTurtle = datasetService.getDataset(DATASET_ID_0, JenaType.TURTLE)
            val responseRDFXML = datasetService.getDataset(DATASET_ID_0, JenaType.RDF_XML)

            val expected = responseReader.parseFile("dataset_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }
}