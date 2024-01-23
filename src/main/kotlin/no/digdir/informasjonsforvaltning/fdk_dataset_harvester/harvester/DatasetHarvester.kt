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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val LOGGER = LoggerFactory.getLogger(DatasetHarvester::class.java)
private const val dateFormat: String = "yyyy-MM-dd HH:mm:ss Z"

@Service
class DatasetHarvester(
    private val adapter: DatasetAdapter,
    private val catalogRepository: CatalogRepository,
    private val datasetRepository: DatasetRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestDatasetCatalog(source: HarvestDataSource, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport? =
        if (source.id != null && source.url != null) {
            try {
                LOGGER.debug("Starting harvest of ${source.url}")

                when (val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)) {
                    null -> {
                        LOGGER.error(
                            "Not able to harvest from ${source.url}, no accept header supplied",
                            HarvestException(source.url)
                        )
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, no accept header supplied",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    Lang.RDFNULL -> {
                        LOGGER.error(
                            "Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable",
                            HarvestException(source.url)
                        )
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, no accept header supplied",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    else -> updateIfChanged(
                        parseRDF(adapter.getDatasets(source), jenaWriterType),
                        source.id, source.url, harvestDate, forceUpdate
                    )
                }
            } catch (ex: Exception) {
                LOGGER.error("Harvest of ${source.url} failed", ex)
                HarvestReport(
                    id = source.id,
                    url = source.url,
                    harvestError = true,
                    errorMessage = ex.message,
                    startTime = harvestDate.formatWithOsloTimeZone(),
                    endTime = formatNowWithOsloTimeZone()
                )
            }
        } else {
            LOGGER.error("Harvest source is not valid", HarvestException("source not valid"))
            null
        }

    private fun updateIfChanged(
        harvested: Model,
        sourceId: String,
        sourceURL: String,
        harvestDate: Calendar,
        forceUpdate: Boolean
    ): HarvestReport {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { safeParseRDF(it, Lang.TURTLE) }

        return if (!forceUpdate && dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = false,
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            LOGGER.debug("Saving data from $sourceURL, and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            updateDB(harvested, harvestDate, sourceId, sourceURL, forceUpdate)
        }
    }

    private fun updateDB(
        harvested: Model,
        harvestDate: Calendar,
        sourceId: String,
        sourceURL: String,
        forceUpdate: Boolean
    ): HarvestReport {
        val updatedCatalogs = mutableListOf<CatalogMeta>()
        val updatedDatasets = mutableListOf<DatasetMeta>()
        val removedDatasets = mutableListOf<DatasetMeta>()
        extractCatalogs(harvested, sourceURL)
            .map { Pair(it, catalogRepository.findByIdOrNull(it.resource.uri)) }
            .filter { forceUpdate || it.first.catalogHasChanges(it.second?.fdkId) }
            .forEach {
                val dbMeta = it.second
                val catalogMeta = if (dbMeta == null || it.first.catalogHasChanges(dbMeta.fdkId)) {
                    val updatedCatalogMeta = it.first.mapToCatalogMeta(harvestDate, dbMeta)
                    catalogRepository.save(updatedCatalogMeta)
                    updatedCatalogMeta
                } else dbMeta
                updatedCatalogs.add(catalogMeta)

                turtleService.saveAsCatalog(
                    model = it.first.harvestedCatalog,
                    fdkId = catalogMeta.fdkId,
                    withRecords = false
                )

                val fdkUri = "${applicationProperties.catalogUri}/${catalogMeta.fdkId}"

                it.first.datasets.forEach { dataset ->
                    dataset.updateDataset(harvestDate, fdkUri, forceUpdate)
                        ?.let { datasetMeta -> updatedDatasets.add(datasetMeta) }
                }

                removedDatasets.addAll(
                    getDatasetsRemovedThisHarvest(
                        fdkUri,
                        it.first.datasets.map { dataset -> dataset.resource.uri }
                    )
                )
            }

        removedDatasets.map { it.copy(removed = true) }.run { datasetRepository.saveAll(this) }

        LOGGER.debug("Harvest of $sourceURL completed")
        return HarvestReport(
            id = sourceId,
            url = sourceURL,
            harvestError = false,
            startTime = harvestDate.formatWithOsloTimeZone(),
            endTime = formatNowWithOsloTimeZone(),
            changedCatalogs = updatedCatalogs.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) },
            changedResources = updatedDatasets.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) },
            removedResources = removedDatasets.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) }
        )
    }

    private fun DatasetModel.updateDataset(
        harvestDate: Calendar,
        fdkCatalogURI: String,
        forceUpdate: Boolean
    ): DatasetMeta? {
        val dbMeta = datasetRepository.findByIdOrNull(resource.uri)
        return when {
            dbMeta == null || datasetHasChanges(dbMeta.fdkId) -> {
                val datasetMeta = mapToMetaDBO(harvestDate, fdkCatalogURI, dbMeta)
                datasetRepository.save(datasetMeta)

                turtleService.saveAsDataset(
                    model = harvestedDataset,
                    fdkId = datasetMeta.fdkId,
                    withRecords = false
                )
                datasetMeta
            }
            forceUpdate -> {
                turtleService.saveAsDataset(
                    model = harvestedDataset,
                    fdkId = dbMeta.fdkId,
                    withRecords = false
                )
                dbMeta
            }
            else -> null
        }
    }

    private fun CatalogAndDatasetModels.mapToCatalogMeta(
        harvestDate: Calendar,
        dbMeta: CatalogMeta?
    ): CatalogMeta {
        val catalogURI = resource.uri
        val fdkId = dbMeta?.fdkId ?: createIdFromString(catalogURI)
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
        val fdkId = dbMeta?.fdkId ?: createIdFromString(resource.uri)
        val issued: Calendar = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return DatasetMeta(
            uri = resource.uri,
            fdkId = fdkId,
            isPartOf = catalogURI,
            removed = false,
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

    private fun formatNowWithOsloTimeZone(): String =
        ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
            .format(DateTimeFormatter.ofPattern(dateFormat))

    private fun Calendar.formatWithOsloTimeZone(): String =
        ZonedDateTime.from(toInstant().atZone(ZoneId.of("Europe/Oslo")))
            .format(DateTimeFormatter.ofPattern(dateFormat))

    private fun getDatasetsRemovedThisHarvest(catalog: String, datasets: List<String>): List<DatasetMeta> =
        datasetRepository.findAllByIsPartOf(catalog)
            .filter { !it.removed && !datasets.contains(it.uri) }
}
