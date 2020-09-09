package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetDBO
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DatasetRepository : MongoRepository<DatasetDBO, String> {
    fun findOneByFdkId(fdkId: String): DatasetDBO?
}