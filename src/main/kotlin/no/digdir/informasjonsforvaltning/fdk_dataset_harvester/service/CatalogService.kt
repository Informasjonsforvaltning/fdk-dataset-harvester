package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.DatasetFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.addDefaultPrefixes
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createIdFromUri
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class CatalogService(private val catalogFuseki: CatalogFuseki, private val datasetFuseki: DatasetFuseki) {

    fun countDatasetCatalogs(): Int =
        catalogFuseki.fetchCompleteModel()
            .listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .size

    fun getAllDatasetCatalogs(returnType: JenaType): String =
        catalogFuseki
            .fetchCompleteModel()
            .addAllDatasets()
            .addDefaultPrefixes()
            .createRDFResponse(returnType)

    fun getDatasetCatalog(id: String, returnType: JenaType): String? =
        catalogFuseki
            .fetchByGraphName(id)
            ?.addDatasets()
            ?.addDefaultPrefixes()
            ?.createRDFResponse(returnType)

    private fun Model.addAllDatasets(): Model =
        union(datasetFuseki.fetchCompleteModel())

    private fun Model.addDatasets(): Model {

        var unionModel = ModelFactory.createDefaultModel().union(this)

        val datasetIdList = mutableListOf<String>()

        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .forEach { catalog ->
                catalog.listProperties(DCAT.dataset)
                    .toList()
                    .forEach { dataset ->
                        datasetIdList.add(createIdFromUri(dataset.resource.uri))
                    } }

        datasetIdList
            .toList()
            .mapNotNull { id -> datasetFuseki.fetchByGraphName(id) }
            .forEach { unionModel = unionModel.union(it) }

        return unionModel
    }

}