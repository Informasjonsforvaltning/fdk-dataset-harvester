package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.FDKDatasetTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FDKDatasetTurtleRepository : MongoRepository<FDKDatasetTurtle, String>
