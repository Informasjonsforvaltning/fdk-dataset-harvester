package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester.formatNowWithOsloTimeZone
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DuplicateIRI
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FdkIdAndUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestReport
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit.RabbitMQPublisher
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class DatasetService(
    private val datasetRepository: DatasetRepository,
    private val rabbitPublisher: RabbitMQPublisher,
    private val turtleService: TurtleService
) {

    fun getAll(returnType: Lang, withRecords: Boolean): String =
        turtleService.getCatalogUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDF(it, Lang.TURTLE).createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getDataset(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getDataset(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDF(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun getDatasetCatalog(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getCatalog(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDF(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun removeDataset(id: String) {
        val start = formatNowWithOsloTimeZone()
        val meta = datasetRepository.findAllByFdkId(id)
        if (meta.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No Dataset found with fdkID $id")
        } else if (meta.none { !it.removed }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Dataset with fdkID $id has already been removed")
        } else {
            datasetRepository.saveAll(meta.map { it.copy(removed = true) })

            val uri = meta.first().uri
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "manual-delete-$id",
                    url = uri,
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = listOf(FdkIdAndUri(fdkId = id, uri = uri))
                )
            ))
        }
    }

    fun removeDuplicates(duplicates: List<DuplicateIRI>) {
        val start = formatNowWithOsloTimeZone()
        val reportAsRemoved: MutableList<FdkIdAndUri> = mutableListOf()

        duplicates.flatMap { duplicate ->
            val remove = datasetRepository.findByIdOrNull(duplicate.iriToRemove)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No dataset connected to IRI ${duplicate.iriToRemove}")

            val retain = datasetRepository.findByIdOrNull(duplicate.iriToRetain)
                ?.let { if (it.issued > remove.issued) it.copy(issued = remove.issued) else it } // keep earliest issued
                ?.let { if (it.modified < remove.modified) it.copy(modified = remove.modified) else it } // keep latest modified
                ?.let {
                    if (duplicate.keepRemovedFdkId) {
                        if (it.removed) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Dataset with IRI ${it.uri} has already been removed")
                        reportAsRemoved.add(FdkIdAndUri(fdkId = it.fdkId, uri = it.uri))
                        it.copy(fdkId = remove.fdkId)
                    } else {
                        if (remove.removed) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Dataset with IRI ${remove.uri} has already been removed")
                        reportAsRemoved.add(FdkIdAndUri(fdkId = remove.fdkId, uri = remove.uri))
                        it
                    }
                }
                ?: remove.copy(uri = duplicate.iriToRetain)

            listOf(remove.copy(removed = true), retain.copy(removed = false))
        }.run { datasetRepository.saveAll(this) }

        if (reportAsRemoved.isNotEmpty()) {
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "duplicate-delete",
                    url = "https://fellesdatakatalog.digdir.no/duplicates",
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = reportAsRemoved
                )
            ))
        }
    }

    // Purges everything associated with a removed fdkID
    fun purgeByFdkId(fdkId: String) {
        datasetRepository.findAllByFdkId(fdkId)
            .also { datasets -> if (datasets.any { !it.removed }) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to purge files, dataset with id $fdkId has not been removed") }
            .run { datasetRepository.deleteAll(this) }

        turtleService.deleteTurtleFiles(fdkId)
    }

}
