package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.safeParseRDF
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.ungzip
import org.apache.jena.riot.Lang
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "datasetMeta")
data class DatasetMeta (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String,
    val removed: Boolean = false,

    val issued: Long,
    val modified: Long
)

@Document(collection = "catalogMeta")
data class CatalogMeta (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val issued: Long,
    val modified: Long
)

@Document(collection = "turtle")
data class TurtleDBO(
    @Id
    val id: String,
    val turtle: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TurtleDBO

        return when {
            id != other.id -> false
            else -> zippedModelsAreIsomorphic(turtle, other.turtle)
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsomorphic(zip0: String, zip1: String): Boolean {
    val model0 = safeParseRDF(ungzip(zip0), Lang.TURTLE)
    val model1 = safeParseRDF(ungzip(zip1), Lang.TURTLE)

    return model0.isIsomorphicWith(model1)
}
