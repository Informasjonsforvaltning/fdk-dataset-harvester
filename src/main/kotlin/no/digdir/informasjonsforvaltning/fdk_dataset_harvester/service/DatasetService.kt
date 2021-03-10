package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.UNION_ID
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.MiscellaneousRepository
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(DatasetService::class.java)

@Service
class DatasetService(
    private val catalogRepository: CatalogRepository,
    private val datasetRepository: DatasetRepository,
    private val miscellaneousRepository: MiscellaneousRepository
) {

    fun countMetaData(): Long =
        catalogRepository.count()

    fun getAll(returnType: Lang): String =
        miscellaneousRepository.findByIdOrNull(UNION_ID)
            ?.let { ungzip(it.turtle) }
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getDataset(id: String, returnType: Lang): String? =
        datasetRepository.findOneByFdkId(id)
            ?.let { ungzip(it.turtleDataset) }
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

    fun getDatasetCatalog(id: String, returnType: Lang): String? =
        catalogRepository.findOneByFdkId(id)
            ?.let { ungzip(it.turtleCatalog) }
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

}