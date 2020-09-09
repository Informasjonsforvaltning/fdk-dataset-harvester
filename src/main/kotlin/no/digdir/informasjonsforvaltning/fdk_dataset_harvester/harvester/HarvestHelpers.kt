package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetDBO
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


fun CatalogAndDatasetModels.catalogDiffersFromDB(dbo: CatalogDBO?): Boolean =
    if (dbo == null) true
    else !harvestedCatalog.isIsomorphicWith(parseRDFResponse(dbo.turtleHarvested, JenaType.TURTLE, null))

fun DatasetModel.differsFromDB(dbo: DatasetDBO): Boolean =
    !harvestedDataset.isIsomorphicWith(parseRDFResponse(dbo.turtleHarvested, JenaType.TURTLE, null))

fun Resource.parsePropertyToCalendar(property: Property): List<Calendar>? =
    listProperties(property)
        ?.toList()
        ?.mapNotNull { it.string }
        ?.map {
            val cal = Calendar.getInstance()
            val ldt = LocalDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            cal.time = Date.from(ldt.atZone(ZoneId.of("Z")).toInstant())
            cal
        }

fun Resource.addModified(modified: List<Calendar>) {
    modified.forEach { addProperty(DCTerms.modified, model.createTypedLiteral(it)) }
}

fun splitCatalogsFromModel(harvested: Model): List<CatalogAndDatasetModels> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .map { catalogResource ->
            val catalogDatasets: List<DatasetModel> = catalogResource.listProperties(DCAT.dataset)
                .toList()
                .map { dataset -> dataset.resource.extractDataset() }

            var catalogModelWithoutDatasets = catalogResource.listProperties().toModel()
            catalogModelWithoutDatasets.addDefaultPrefixes()

            catalogResource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.dataset) {
                        catalogModelWithoutDatasets = catalogModelWithoutDatasets.addNonDatasetResourceToModel(it.resource)
                    }
                }

            var catalogModel = catalogModelWithoutDatasets
            catalogDatasets.forEach { catalogModel = catalogModel.union(it.harvestedDataset) }

            CatalogAndDatasetModels(
                resource = catalogResource,
                harvestedCatalog = catalogModel,
                harvestedCatalogWithoutDatasets = catalogModelWithoutDatasets,
                datasets = catalogDatasets
            )
        }

fun Resource.extractDataset(): DatasetModel {
    var datasetModel = listProperties().toModel()
    datasetModel = datasetModel.addDefaultPrefixes()

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach {
            if (it.predicate != DCAT.distribution && it != DQV.hasQualityAnnotation) {
                datasetModel = datasetModel.addNonDatasetResourceToModel(it.resource)
            }
        }

    datasetModel = datasetModel.union(modelOfDistributionProperties())
    datasetModel = datasetModel.union(modelOfQualityProperties())

    return DatasetModel(resource = this, harvestedDataset = datasetModel)
}

private fun Model.addNonDatasetResourceToModel(resource: Resource): Model {
    val types = resource.listProperties(RDF.type)
        .toList()
        .map { it.`object` }

    if (!types.contains(DCAT.Dataset)) {

        add(resource.listProperties())

        resource.listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach {
                add(it.resource.listProperties())
            }
    }

    return this
}

data class CatalogAndDatasetModels (
    val resource: Resource,
    val harvestedCatalog: Model,
    val harvestedCatalogWithoutDatasets: Model,
    val datasets: List<DatasetModel>,
)

data class DatasetModel (
    val resource: Resource,
    val harvestedDataset: Model
)