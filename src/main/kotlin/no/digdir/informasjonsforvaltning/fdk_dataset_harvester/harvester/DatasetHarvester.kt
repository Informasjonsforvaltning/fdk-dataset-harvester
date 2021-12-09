package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.DatasetAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.TurtleService
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(DatasetHarvester::class.java)

@Service
class DatasetHarvester(
    private val adapter: DatasetAdapter,
    private val catalogRepository: CatalogRepository,
    private val datasetRepository: DatasetRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestDatasetCatalog(source: HarvestDataSource, harvestDate: Calendar) =
    if (source.url != null) {
        LOGGER.debug("Starting harvest of ${source.url}")
        val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

        val harvested = when (jenaWriterType) {
            null -> null
            Lang.RDFNULL -> null
            else -> adapter.getDatasets(source)?.let { parseRDFResponse(it, jenaWriterType, source.url) }
        }

        when {
            jenaWriterType == null -> LOGGER.error("Not able to harvest from ${source.url}, no accept header supplied", HarvestException(source.url))
            jenaWriterType == Lang.RDFNULL -> LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable", HarvestException(source.url))
            harvested == null -> LOGGER.warn("Not able to harvest ${source.url}")
            else -> updateIfChanged(harvested, source.url, harvestDate)
        }
    } else LOGGER.error("Harvest source is not defined", HarvestException("undefined"))

    private fun updateIfChanged(harvested: Model, sourceURL: String, harvestDate: Calendar) {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        if (dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
        } else {
            LOGGER.debug("Changes detected, saving data from $sourceURL, and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            updateDB(harvested, harvestDate, sourceURL)
            LOGGER.debug("Harvest of $sourceURL completed")
        }
    }

    private fun updateDB(harvested: Model, harvestDate: Calendar, sourceURL: String) {
        extractCatalogs(harvested, sourceURL)
            .map { Pair(it, catalogRepository.findByIdOrNull(it.resource.uri)) }
            .filter { it.first.catalogHasChanges(it.second?.fdkId) }
            .forEach {
                val updatedCatalogMeta = it.first.mapToCatalogMeta(harvestDate, it.second)
                catalogRepository.save(updatedCatalogMeta)

                turtleService.saveAsCatalog(
                    model = it.first.harvestedCatalog,
                    fdkId = updatedCatalogMeta.fdkId,
                    withRecords = false
                )

                val fdkUri = "${applicationProperties.catalogUri}/${updatedCatalogMeta.fdkId}"

                it.first.datasets.forEach { dataset ->
                    dataset.updateDataset(harvestDate, fdkUri)
                }
            }
    }

    private fun DatasetModel.updateDataset(
        harvestDate: Calendar,
        fdkCatalogURI: String
    ) {
        val dbMeta = datasetRepository.findByIdOrNull(resource.uri)
        if (datasetHasChanges(dbMeta?.fdkId)) {
            val modelMeta = mapToMetaDBO(harvestDate, fdkCatalogURI, dbMeta)
            datasetRepository.save(modelMeta)

            turtleService.saveAsDataset(
                model = harvestedDataset,
                fdkId = modelMeta.fdkId,
                withRecords = false
            )
        }
    }

    private fun CatalogAndDatasetModels.mapToCatalogMeta(
        harvestDate: Calendar,
        dbMeta: CatalogMeta?
    ): CatalogMeta {
        val catalogURI = resource.uri
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(catalogURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return CatalogMeta(
            uri = catalogURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun DatasetModel.mapToMetaDBO(
        harvestDate: Calendar,
        catalogURI: String,
        dbMeta: DatasetMeta?
    ): DatasetMeta {
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(resource.uri)
        val issued: Calendar = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return DatasetMeta(
            uri = resource.uri,
            fdkId = fdkId,
            isPartOf = catalogURI,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun CatalogAndDatasetModels.catalogHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getCatalog(fdkId, withRecords = false))

    private fun DatasetModel.datasetHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getDataset(fdkId, withRecords = false))
}
