package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogDBO
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CatalogRepository : MongoRepository<CatalogDBO, String> {
    fun findOneByFdkId(fdkId: String): CatalogDBO?
}