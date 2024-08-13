package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DatasetRepository : MongoRepository<DatasetMeta, String> {
    fun findAllByIsPartOf(isPartOf: String): List<DatasetMeta>
    fun findAllByFdkId(fdkId: String): List<DatasetMeta>
}
