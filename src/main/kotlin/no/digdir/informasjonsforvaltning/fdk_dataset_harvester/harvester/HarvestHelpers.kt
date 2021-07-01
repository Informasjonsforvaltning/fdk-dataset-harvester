package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF

fun CatalogAndDatasetModels.harvestDiff(dbNoRecords: String?): Boolean =
    if (dbNoRecords == null) true
    else !harvestedCatalog.isIsomorphicWith(parseRDFResponse(dbNoRecords, Lang.TURTLE, null))

fun DatasetModel.harvestDiff(dbNoRecords: String?): Boolean =
    if (dbNoRecords == null) true
    else !harvestedDataset.isIsomorphicWith(parseRDFResponse(dbNoRecords, Lang.TURTLE, null))

fun extractCatalogs(harvested: Model): List<CatalogAndDatasetModels> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .map { catalogResource ->
            val catalogDatasets: List<DatasetModel> = catalogResource.listProperties(DCAT.dataset)
                .toList()
                .map { dataset -> dataset.resource.extractDataset() }

            var catalogModelWithoutDatasets = catalogResource.listProperties().toModel()
            catalogModelWithoutDatasets.setNsPrefixes(harvested.nsPrefixMap)

            catalogResource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.dataset) {
                        catalogModelWithoutDatasets = catalogModelWithoutDatasets.recursiveAddNonDatasetResource(it.resource, 5)
                    }
                }

            var datasetsUnion = ModelFactory.createDefaultModel()
            catalogDatasets.forEach { datasetsUnion = datasetsUnion.union(it.harvestedDataset) }

            CatalogAndDatasetModels(
                resource = catalogResource,
                harvestedCatalog = catalogModelWithoutDatasets.union(datasetsUnion),
                harvestedCatalogWithoutDatasets = catalogModelWithoutDatasets,
                datasets = catalogDatasets
            )
        }

fun Resource.extractDataset(): DatasetModel {
    var datasetModel = listProperties().toModel()
    datasetModel = datasetModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach {
            datasetModel = datasetModel.recursiveAddNonDatasetResource(it.resource, 10)
        }

    return DatasetModel(resource = this, harvestedDataset = datasetModel)
}

private fun Model.recursiveAddNonDatasetResource(resource: Resource, recursiveCount: Int): Model {
    val newCount = recursiveCount - 1

    if (resourceShouldBeAdded(resource)) {
        add(resource.listProperties())

        if (newCount > 0) {
            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonDatasetResource(it.resource, newCount) }
        }
    }

    return this
}

private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
    val types = resource.listProperties(RDF.type)
        .toList()
        .map { it.`object` }

    return when {
        types.contains(DCAT.Dataset) -> false
        containsTriple("<${resource.uri}>", "a", "?o") -> false
        else -> true
    }
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

class HarvestException(url: String) : Exception("Harvest failed for $url")
