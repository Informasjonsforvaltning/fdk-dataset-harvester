package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FDKCatalogTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FDKDatasetTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestSourceTurtle
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.TurtleDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogTurtleRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetTurtleRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.FDKCatalogTurtleRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.FDKDatasetTurtleRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.HarvestSourceTurtleRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

const val UNION_ID = "union-graph"

@Service
class TurtleService(
    private val catalogTurtleRepository: CatalogTurtleRepository,
    private val datasetTurtleRepository: DatasetTurtleRepository,
    private val fdkCatalogTurtleRepository: FDKCatalogTurtleRepository,
    private val fdkDatasetTurtleRepository: FDKDatasetTurtleRepository,
    private val harvestSourceTurtleRepository: HarvestSourceTurtleRepository
) {

    fun saveAsCatalogUnion(model: Model, withRecords: Boolean): TurtleDBO =
        if (withRecords) fdkCatalogTurtleRepository.save(model.createFDKCatalogTurtleDBO(UNION_ID))
        else catalogTurtleRepository.save(model.createCatalogTurtleDBO(UNION_ID))

    fun getCatalogUnion(withRecords: Boolean): String? =
        if (withRecords) fdkCatalogTurtleRepository.findByIdOrNull(UNION_ID)
            ?.turtle
            ?.let { ungzip(it) }
        else catalogTurtleRepository.findByIdOrNull(UNION_ID)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsCatalog(model: Model, fdkId: String, withRecords: Boolean): TurtleDBO =
        if (withRecords) fdkCatalogTurtleRepository.save(model.createFDKCatalogTurtleDBO(fdkId))
        else catalogTurtleRepository.save(model.createCatalogTurtleDBO(fdkId))

    fun getCatalog(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkCatalogTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else catalogTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsDataset(model: Model, fdkId: String, withRecords: Boolean): TurtleDBO =
        if (withRecords) fdkDatasetTurtleRepository.save(model.createFDKDatasetTurtleDBO(fdkId))
        else datasetTurtleRepository.save(model.createDatasetTurtleDBO(fdkId))

    fun getDataset(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkDatasetTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else datasetTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(model: Model, uri: String): TurtleDBO =
        harvestSourceTurtleRepository.save(model.createHarvestSourceTurtleDBO(uri))

    fun getHarvestSource(uri: String): String? =
        harvestSourceTurtleRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

    fun deleteTurtleFiles(fdkId: String) {
        datasetTurtleRepository.deleteById(fdkId)
        fdkDatasetTurtleRepository.deleteById(fdkId)
    }
}

private fun Model.createHarvestSourceTurtleDBO(uri: String): HarvestSourceTurtle =
    HarvestSourceTurtle(
        id = uri,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createCatalogTurtleDBO(fdkId: String): CatalogTurtle =
    CatalogTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createDatasetTurtleDBO(fdkId: String): DatasetTurtle =
    DatasetTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createFDKCatalogTurtleDBO(fdkId: String): FDKCatalogTurtle =
    FDKCatalogTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createFDKDatasetTurtleDBO(fdkId: String): FDKDatasetTurtle =
    FDKDatasetTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = Base64.getDecoder().decode(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(UTF_8)
        .use { it.readText() }
}
