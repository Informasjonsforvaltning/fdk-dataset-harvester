package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CatalogTurtleRepository : MongoRepository<CatalogTurtle, String>
