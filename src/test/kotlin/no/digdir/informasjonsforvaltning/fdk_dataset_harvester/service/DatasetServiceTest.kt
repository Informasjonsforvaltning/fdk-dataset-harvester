package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.DATASET_ID_0
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class DatasetServiceTest {
    private val datasetFuseki: DatasetFuseki = mock()
    private val datasetService = DatasetService(datasetFuseki)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllDatasets {

        @Test
        fun answerWithEmptyListWhenNoModelsSavedInFuseki() {
            whenever(datasetFuseki.fetchCompleteModel())
                .thenReturn(ModelFactory.createDefaultModel())

            val expected = responseReader.parseResponse("", "TURTLE")

            val response = datasetService.getAllDatasets(JenaType.TURTLE)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

        @Test
        fun responseIsIsomorphicWithModelFromFuseki() {
            val db0 = responseReader.parseFile("db_dataset_0.json", "JSONLD")
            val db1 = responseReader.parseFile("db_dataset_1.json", "JSONLD")
            val dbModel = db0.union(db1)

            whenever(datasetFuseki.fetchCompleteModel())
                .thenReturn(dbModel)

            val response = datasetService.getAllDatasets(JenaType.TURTLE)

            assertTrue(dbModel.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }
    }

    @Nested
    internal inner class DatasetById {

        @Test
        fun responseIsNullWhenNotFoundInFuseki() {
            whenever(datasetFuseki.fetchByGraphName("123"))
                .thenReturn(null)

            val response = datasetService.getDataset("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithModelFromFuseki() {
            val dbModel = responseReader.parseFile("db_dataset_0.json", "JSONLD")
            whenever(datasetFuseki.fetchByGraphName(DATASET_ID_0))
                .thenReturn(dbModel)

            val response = datasetService.getDataset(DATASET_ID_0, JenaType.TURTLE)

            assertTrue(dbModel.isIsomorphicWith(responseReader.parseResponse(response!!, "TURTLE")))
        }

    }
}