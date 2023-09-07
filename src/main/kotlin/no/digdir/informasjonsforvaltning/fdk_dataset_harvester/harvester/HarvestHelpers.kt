package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.Application
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun CatalogAndDatasetModels.harvestDiff(dbNoRecords: String?): Boolean =
    if (dbNoRecords == null) true
    else !harvestedCatalog.isIsomorphicWith(safeParseRDF(dbNoRecords, Lang.TURTLE))

fun DatasetModel.harvestDiff(dbNoRecords: String?): Boolean =
    if (dbNoRecords == null) true
    else !harvestedDataset.isIsomorphicWith(safeParseRDF(dbNoRecords, Lang.TURTLE))

private fun Model.recursiveBlankNodeSkolem(baseURI: String): Model {
    val anonSubjects = listSubjects().toList().filter { it.isAnon }
    if (anonSubjects.isEmpty()) return this
    else {
        anonSubjects
            .filter { it.doesNotContainAnon() }
            .forEach { ResourceUtils.renameResource(it, "$baseURI/.well-known/skolem/${it.createSkolemID()}") }
        return this.recursiveBlankNodeSkolem(baseURI)
    }
}

private fun Resource.doesNotContainAnon(): Boolean =
    listProperties().toList()
        .filter { it.isResourceProperty() }
        .none { it.resource.isAnon }

private fun Resource.createSkolemID(): String =
    createIdFromString(
        listProperties().toModel()
            .createRDFResponse(Lang.N3)
            .replace("\\s".toRegex(), "")
            .toCharArray()
            .sorted()
            .toString()
    )

fun extractCatalogs(harvested: Model, sourceURL: String): List<CatalogAndDatasetModels> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .filterBlankNodeCatalogsAndDatasets(sourceURL)
        .map { catalogResource ->
            val catalogDatasets: List<DatasetModel> = catalogResource.listProperties(DCAT.dataset)
                .toList()
                .filter { it.isResourceProperty() }
                .map { it.resource }
                .flatMap { it.extractDatasetsInSeries() }
                .filter { it.isDataset() }
                .filterBlankNodeCatalogsAndDatasets(sourceURL)
                .map { it.extractDataset() }

            val catalogModelWithoutDatasets = ModelFactory.createDefaultModel()
            catalogModelWithoutDatasets.setNsPrefixes(harvested.nsPrefixMap)

            catalogResource.listProperties()
                .toList()
                .forEach { catalogModelWithoutDatasets.addCatalogProperties(it) }

            catalogModelWithoutDatasets.recursiveBlankNodeSkolem(catalogResource.uri)

            val datasetsUnion = ModelFactory.createDefaultModel()
            catalogDatasets.forEach { datasetsUnion.add(it.harvestedDataset) }

            CatalogAndDatasetModels(
                resource = catalogResource,
                harvestedCatalog = catalogModelWithoutDatasets.union(datasetsUnion),
                harvestedCatalogWithoutDatasets = catalogModelWithoutDatasets,
                datasets = catalogDatasets
            )
        }

private fun List<Resource>.filterBlankNodeCatalogsAndDatasets(sourceURL: String): List<Resource> =
    filter {
        if (it.isURIResource) true
        else {
            LOGGER.error(
                "Failed harvest of catalog or dataset for $sourceURL, unable to harvest blank node catalogs and dataset",
                Exception("unable to harvest blank node catalogs and datasets")
            )
            false
        }
    }

private fun Model.addCatalogProperties(property: Statement): Model =
    when {
        property.predicate != DCAT.dataset && property.isResourceProperty() ->
            add(property).recursiveAddNonDatasetResource(property.resource, 5)
        property.predicate != DCAT.dataset -> add(property)
        property.isResourceProperty() && property.resource.isURIResource -> add(property)
        else -> this
    }

fun Resource.extractDataset(): DatasetModel {
    val datasetModel = listProperties().toModel()
    datasetModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach { datasetModel.recursiveAddNonDatasetResource(it.resource, 10) }

    return DatasetModel(resource = this, harvestedDataset = datasetModel.recursiveBlankNodeSkolem(uri))
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
        types.contains(DCAT3.DatasetSeries) -> false
        !resource.isURIResource -> true
        containsTriple("<${resource.uri}>", "a", "?o") -> false
        else -> true
    }
}

private fun Resource.extractDatasetsInSeries(): List<Resource> {
    val types = listProperties(RDF.type)
        .toList()
        .map { it.`object` }

    return if (types.contains(DCAT3.DatasetSeries)) {
        val datasetsInSeries = model.listResourcesWithProperty(DCAT3.inSeries, this)
            .toList()
        datasetsInSeries.add(this)
        datasetsInSeries
    } else {
        listOf(this)
    }
}

private fun Resource.isDataset(): Boolean {
    val types = listProperties(RDF.type)
        .toList()
        .map { it.`object` }

    return when {
        types.contains(DCAT.Dataset) -> true
        types.contains(DCAT3.DatasetSeries) -> true
        else -> false
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
