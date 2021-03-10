package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import org.apache.jena.rdf.model.Model
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
    datasetModel = datasetModel.setNsPrefixes(model.nsPrefixMap)

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
