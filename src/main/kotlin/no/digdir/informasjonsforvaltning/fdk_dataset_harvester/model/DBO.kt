package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.parseRDFResponse
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.ungzip
import org.apache.jena.riot.Lang
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document


const val UNION_ID = "dataset-catalogs-union-graph"

@Document(collection = "dataset")
data class DatasetDBO (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String,
    val issued: Long,
    val modified: Long,

    val turtleHarvested: String,
    val turtleDataset: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DatasetDBO

        return when {
            uri != other.uri -> false
            fdkId != other.fdkId -> false
            isPartOf != other.isPartOf -> false
            issued != other.issued -> false
            modified != other.modified -> false
            !zippedModelsAreIsometric(turtleHarvested, other.turtleHarvested) -> false
            else -> zippedModelsAreIsometric(turtleDataset, other.turtleDataset)
        }
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fdkId.hashCode()
        result = 31 * result + isPartOf.hashCode()
        result = 31 * result + issued.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + turtleHarvested.hashCode()
        result = 31 * result + turtleDataset.hashCode()
        return result
    }
}

@Document(collection = "catalog")
data class CatalogDBO (
        @Id
        val uri: String,

        @Indexed(unique = true)
        val fdkId: String,

        val issued: Long,
        val modified: Long,

        val turtleHarvested: String,
        val turtleCatalog: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CatalogDBO

        return when {
            uri != other.uri -> false
            fdkId != other.fdkId -> false
            issued != other.issued -> false
            modified != other.modified -> false
            !zippedModelsAreIsometric(turtleHarvested, other.turtleHarvested) -> false
            else -> zippedModelsAreIsometric(turtleCatalog, other.turtleCatalog)
        }
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fdkId.hashCode()
        result = 31 * result + issued.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + turtleHarvested.hashCode()
        result = 31 * result + turtleCatalog.hashCode()
        return result
    }
}

@Document(collection = "misc")
data class MiscellaneousTurtle (
        @Id val id: String,
        val isHarvestedSource: Boolean,
        val turtle: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MiscellaneousTurtle

        return when {
            id != other.id -> false
            isHarvestedSource != other.isHarvestedSource -> false
            else -> zippedModelsAreIsometric(turtle, other.turtle)
        }

    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isHarvestedSource.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsometric(zip0: String, zip1: String): Boolean {
    val model0 = parseRDFResponse(ungzip(zip0), Lang.TURTLE, null)
    val model1 = parseRDFResponse(ungzip(zip1), Lang.TURTLE, null)

    return when {
        model0 != null && model1 != null -> model0.isIsomorphicWith(model1)
        model0 == null && model1 == null -> true
        else -> false
    }
}
