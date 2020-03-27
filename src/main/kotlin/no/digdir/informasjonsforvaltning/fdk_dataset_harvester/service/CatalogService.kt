package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.createRDFResponse
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class CatalogService(private val catalogFuseki: CatalogFuseki) {

    fun countDatasetCatalogs(): Int =
        catalogFuseki.fetchCompleteModel()
            .listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .size

    fun getAllDatasetCatalogs(): String =
        catalogFuseki
            .fetchCompleteModel()
            .createRDFResponse()

    fun getDatasetCatalog(id: String): String? =
        catalogFuseki
            .fetchByGraphName(id)
            ?.createRDFResponse()

}