package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.fuseki.CatalogFuseki
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.JenaType
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

    fun getAllDatasetCatalogs(returnType: JenaType): String =
        catalogFuseki
            .fetchCompleteModel()
            .createRDFResponse(returnType)

    fun getDatasetCatalog(id: String, returnType: JenaType): String? =
        catalogFuseki
            .fetchByGraphName(id)
            ?.createRDFResponse(returnType)

}