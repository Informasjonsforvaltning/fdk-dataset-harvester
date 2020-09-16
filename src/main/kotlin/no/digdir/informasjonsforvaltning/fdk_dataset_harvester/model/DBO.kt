package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.DateDeserializers
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.parseRDFResponse
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service.ungzip
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Calendar


const val UNION_ID = "dataset-catalogs-union-graph"

@Document(collection = "dataset")
data class DatasetDBO (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String,

    @JsonDeserialize(using = DateDeserializers.CalendarDeserializer::class)
    @JsonSerialize(using = CalendarSerializer::class)
    val issued: Calendar,

    @JsonDeserialize(using = DateDeserializers.CalendarDeserializer::class)
    @JsonSerialize(using = CalendarSerializer::class)
    val modified: Calendar,

    val turtleHarvested: ByteArray,
    val turtleDataset: ByteArray
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
        result = 31 * result + turtleHarvested.contentHashCode()
        result = 31 * result + turtleDataset.contentHashCode()
        return result
    }
}

@Document(collection = "catalog")
data class CatalogDBO (
        @Id
        val uri: String,

        @Indexed(unique = true)
        val fdkId: String,

        @JsonDeserialize(using = DateDeserializers.CalendarDeserializer::class)
        @JsonSerialize(using = CalendarSerializer::class)
        val issued: Calendar,

        @JsonDeserialize(using = DateDeserializers.CalendarDeserializer::class)
        @JsonSerialize(using = CalendarSerializer::class)
        val modified: Calendar,

        val turtleHarvested: ByteArray,
        val turtleCatalog: ByteArray
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
        result = 31 * result + turtleHarvested.contentHashCode()
        result = 31 * result + turtleCatalog.contentHashCode()
        return result
    }
}

@Document(collection = "misc")
data class MiscellaneousTurtle (
        @Id val id: String,
        val isHarvestedSource: Boolean,
        val turtle: ByteArray
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
        result = 31 * result + turtle.contentHashCode()
        return result
    }
}

private fun zippedModelsAreIsometric(zip0: ByteArray, zip1: ByteArray): Boolean {
    val model0 = parseRDFResponse(ungzip(zip0), JenaType.TURTLE, null)
    val model1 = parseRDFResponse(ungzip(zip1), JenaType.TURTLE, null)

    return when {
        model0 != null && model1 != null -> model0.isIsomorphicWith(model1)
        model0 == null && model1 == null -> true
        else -> false
    }
}
