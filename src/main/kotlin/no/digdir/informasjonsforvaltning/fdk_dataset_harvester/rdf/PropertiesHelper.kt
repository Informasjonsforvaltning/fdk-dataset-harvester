package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF

val datasetPropertyPaths = listOf(
    "dcat:dataset/dcat:distribution",
    "dcat:dataset/dcat:distribution/dcatapi:accessService",
    "dcat:dataset/dct:publisher",
    "dcat:dataset/dcat:contactPoint",
    "dcat:dataset/dct:spatial"
)

fun changedCatalogAndDatasets(harvested: Model, dbModel: Model?): Map<String, List<String>> {
    val catalogMap = mutableMapOf<String, List<String>>()
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .forEach { catalog ->
            catalogMap[catalog.uri] = catalog.listProperties(DCAT.dataset)
                .toList()
                .map { dataset -> dataset.resource.uri }
                .filter {
                    if (dbModel != null) datasetDiffersInModels(it, harvested, dbModel)
                    else true
                }
        }

    return catalogMap.toMap()
}

fun datasetDiffersInModels(datasetURI: String, harvested: Model, fromDB: Model): Boolean {
    val harvestedDataset = harvested.getResource(datasetURI)
    val obj: RDFNode? = null
    return if (fromDB.contains(harvestedDataset, null, obj)) {
        val dbDataset = fromDB.getResource(datasetURI)

        when {
            resourceLiteralsDiffers(harvestedDataset, dbDataset) -> true
            propertyDiffers(DCTerms.temporal, harvestedDataset, dbDataset) -> true
            propertyDiffers(DCAT.contactPoint, harvestedDataset, dbDataset) -> true
            propertyDiffers(DCTerms.publisher, harvestedDataset, dbDataset) -> true
            propertyDiffers(DCATNO.informationModel, harvestedDataset, dbDataset) -> true
            propertyDiffers(DCTerms.spatial, harvestedDataset, dbDataset) -> true
            propertyDiffers(DCTerms.conformsTo, harvestedDataset, dbDataset) -> true
            propertyDiffers(ADMS.sample, harvestedDataset, dbDataset) -> true
            distributionsDiffers(harvestedDataset, dbDataset) -> true
            qualityDiffers(harvestedDataset, dbDataset) -> true
            else -> false
        }
    } else true
}

fun resourceLiteralsDiffers(harvested: Resource, db: Resource): Boolean =
    !harvested.listProperties().toModel().isIsomorphicWith(db.listProperties().toModel())

fun propertyDiffers(property: Property, harvested: Resource, db: Resource): Boolean {
    val harvestedPropertiesModel = harvested.modelOfResourceProperties(property)
    val dbPropertiesModel = db.modelOfResourceProperties(property)

    return !harvestedPropertiesModel.isIsomorphicWith(dbPropertiesModel)
}

fun distributionsDiffers(harvested: Resource, db: Resource): Boolean {
    val harvestedDistributionsModel = harvested.modelOfDistributionProperties()
    val dbDistributionsModel = db.modelOfDistributionProperties()

    return !harvestedDistributionsModel.isIsomorphicWith(dbDistributionsModel)
}

fun qualityDiffers(harvested: Resource, db: Resource): Boolean {
    val harvestedQualityModel = harvested.modelOfQualityProperties()
    val dbQualityModel = db.modelOfQualityProperties()

    return !harvestedQualityModel.isIsomorphicWith(dbQualityModel)
}
