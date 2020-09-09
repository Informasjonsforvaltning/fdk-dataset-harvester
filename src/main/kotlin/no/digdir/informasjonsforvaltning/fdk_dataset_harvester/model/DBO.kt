package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.DateDeserializers
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer
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
    val modified: List<Calendar>,

    val turtleHarvested: String,
    val turtleDataset: String
)

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
        val modified: List<Calendar>,

        val turtleHarvested: String,
        val turtleCatalog: String
)

@Document(collection = "misc")
data class MiscellaneousTurtle (
        @Id val id: String,
        val isHarvestedSource: Boolean,
        val turtle: String
)
