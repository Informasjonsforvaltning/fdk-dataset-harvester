package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DuplicateIRI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FdkIdAndUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestReport
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit.RabbitMQPublisher
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class DatasetServiceTest {
    private val repository: DatasetRepository = mock()
    private val publisher: RabbitMQPublisher = mock()
    private val turtleService: TurtleService = mock()
    private val datasetService = DatasetService(repository, publisher, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllCatalogs {

        @Test
        fun responseIsometricWithEmptyModelForEmptyDB() {
            whenever(turtleService.getCatalogUnion(true))
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = datasetService.getAll(Lang.TURTLE, true)
            val responseJsonLD = datasetService.getAll(Lang.JSONLD, true)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            whenever(turtleService.getCatalogUnion(true))
                .thenReturn(javaClass.classLoader.getResource("all_catalogs.ttl")!!.readText())
            whenever(turtleService.getCatalogUnion(false))
                .thenReturn(javaClass.classLoader.getResource("all_catalogs_no_records.ttl")!!.readText())

            val expectedWithRecords = responseReader.parseFile("all_catalogs.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("all_catalogs_no_records.ttl", "TURTLE")

            val responseTurtle = datasetService.getAll(Lang.TURTLE, false)
            val responseN3 = datasetService.getAll(Lang.N3, true)
            val responseNTriples = datasetService.getAll(Lang.NTRIPLES, true)

            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expectedWithRecords.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expectedWithRecords.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class CatalogById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(turtleService.getCatalog("123", true))
                .thenReturn(null)

            val response = datasetService.getDatasetCatalog("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getCatalog(CATALOG_ID_0, true))
                .thenReturn(javaClass.classLoader.getResource("catalog_0.ttl")!!.readText())
            whenever(turtleService.getCatalog(CATALOG_ID_0, false))
                .thenReturn(javaClass.classLoader.getResource("harvest_response_0.ttl")!!.readText())

            val responseTurtle = datasetService.getDatasetCatalog(CATALOG_ID_0, Lang.TURTLE, true)
            val responseJsonRDF = datasetService.getDatasetCatalog(CATALOG_ID_0, Lang.RDFJSON, false)

            val expectedWithRecords = responseReader.parseFile("catalog_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")

            assertTrue(expectedWithRecords.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseJsonRDF!!, "RDF/JSON")))
        }

    }

    @Nested
    internal inner class DatasetById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(turtleService.getDataset("123", true))
                .thenReturn(null)

            val response = datasetService.getDataset("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getDataset(DATASET_ID_0, true))
                .thenReturn(javaClass.classLoader.getResource("dataset_0.ttl")!!.readText())
            whenever(turtleService.getDataset(DATASET_ID_0, false))
                .thenReturn(javaClass.classLoader.getResource("parsed_dataset_0.ttl")!!.readText())

            val responseTurtle = datasetService.getDataset(DATASET_ID_0, Lang.TURTLE, true)
            val responseRDFXML = datasetService.getDataset(DATASET_ID_0, Lang.RDFXML, false)

            val expectedWithRecords = responseReader.parseFile("dataset_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("parsed_dataset_0.ttl", "TURTLE")

            assertTrue(expectedWithRecords.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }

    @Nested
    internal inner class RemoveDatasetById {

        @Test
        fun throwsResponseStatusExceptionWhenNoMetaFoundInDB() {
            whenever(repository.findAllByFdkId("123"))
                .thenReturn(emptyList())

            assertThrows<ResponseStatusException> { datasetService.removeDataset("123") }
        }

        @Test
        fun throwsExceptionWhenNoNonRemovedMetaFoundInDB() {
            whenever(repository.findAllByFdkId(DATASET_ID_0))
                .thenReturn(listOf(DATASET_DBO_0.copy(removed = true)))

            assertThrows<ResponseStatusException> { datasetService.removeDataset(DATASET_ID_0) }
        }

        @Test
        fun updatesMetaAndSendsRabbitReportWhenMetaIsFound() {
            whenever(repository.findAllByFdkId(DATASET_DBO_0.fdkId))
                .thenReturn(listOf(DATASET_DBO_0))

            datasetService.removeDataset(DATASET_DBO_0.fdkId)

            argumentCaptor<List<DatasetMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(DATASET_DBO_0.copy(removed = true)), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "manual-delete-$DATASET_ID_0",
                url = DATASET_DBO_0.uri,
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(DATASET_DBO_0.fdkId, DATASET_DBO_0.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

    }

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun throwsExceptionWhenRemoveIRINotFoundInDB() {
            whenever(repository.findById("https://123.no"))
                .thenReturn(Optional.empty())
            whenever(repository.findById(DATASET_DBO_1.uri))
                .thenReturn(Optional.of(DATASET_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = "https://123.no",
                iriToRetain = DATASET_DBO_1.uri
            )
            assertThrows<ResponseStatusException> { datasetService.removeDuplicates(listOf(duplicateIRI)) }
        }

        @Test
        fun createsNewMetaWhenRetainIRINotFoundInDB() {
            whenever(repository.findById(DATASET_DBO_0.uri))
                .thenReturn(Optional.of(DATASET_DBO_0))
            whenever(repository.findById(DATASET_DBO_1.uri))
                .thenReturn(Optional.empty())

            val duplicateIRI = DuplicateIRI(
                iriToRemove = DATASET_DBO_0.uri,
                iriToRetain = DATASET_DBO_1.uri
            )
            datasetService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<DatasetMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(DATASET_DBO_0.copy(removed = true), DATASET_DBO_0.copy(uri = DATASET_DBO_1.uri)), firstValue)
            }

            verify(publisher, times(0)).send(any())
        }

        @Test
        fun sendsRabbitReportWithRetainFdkIdWhenKeepingRemoveFdkId() {
            whenever(repository.findById(DATASET_DBO_0.uri))
                .thenReturn(Optional.of(DATASET_DBO_0))
            whenever(repository.findById(DATASET_DBO_1.uri))
                .thenReturn(Optional.of(DATASET_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = DATASET_DBO_0.uri,
                iriToRetain = DATASET_DBO_1.uri
            )
            datasetService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<DatasetMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    DATASET_DBO_0.copy(removed = true),
                    DATASET_DBO_0.copy(uri = DATASET_DBO_1.uri, isPartOf = DATASET_DBO_1.isPartOf)
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(DATASET_DBO_1.fdkId, DATASET_DBO_1.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

        @Test
        fun sendsRabbitReportWithRemoveFdkIdWhenNotKeepingRemoveFdkId() {
            whenever(repository.findById(DATASET_DBO_0.uri))
                .thenReturn(Optional.of(DATASET_DBO_0))
            whenever(repository.findById(DATASET_DBO_1.uri))
                .thenReturn(Optional.of(DATASET_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = DATASET_DBO_1.uri,
                iriToRetain = DATASET_DBO_0.uri,
                keepRemovedFdkId = false
            )
            datasetService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<DatasetMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    DATASET_DBO_1.copy(removed = true),
                    DATASET_DBO_0
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(DATASET_DBO_1.fdkId, DATASET_DBO_1.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

        @Test
        fun throwsExceptionWhenTryingToReportAlreadyRemovedAsRemoved() {
            whenever(repository.findById(DATASET_DBO_0.uri))
                .thenReturn(Optional.of(DATASET_DBO_0.copy(removed = true)))
            whenever(repository.findById(DATASET_DBO_1.uri))
                .thenReturn(Optional.of(DATASET_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = DATASET_DBO_0.uri,
                iriToRetain = DATASET_DBO_1.uri,
                keepRemovedFdkId = false
            )

            assertThrows<ResponseStatusException> { datasetService.removeDuplicates(listOf(duplicateIRI)) }

            whenever(repository.findById(DATASET_DBO_0.uri))
                .thenReturn(Optional.of(DATASET_DBO_0))
            whenever(repository.findById(DATASET_DBO_1.uri))
                .thenReturn(Optional.of(DATASET_DBO_1.copy(removed = true)))

            assertThrows<ResponseStatusException> { datasetService.removeDuplicates(listOf(duplicateIRI.copy(keepRemovedFdkId = true))) }
        }

    }
}
