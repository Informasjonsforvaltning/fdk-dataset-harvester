package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.TurtleDBO
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TurtleRepository : MongoRepository<TurtleDBO, String>
