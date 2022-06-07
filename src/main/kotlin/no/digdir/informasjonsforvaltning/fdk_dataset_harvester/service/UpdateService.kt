package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.CatalogMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.DatasetMeta
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.*
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.CatalogRepository
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.repository.DatasetRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val applicationProperties: ApplicationProperties,
    private val catalogRepository: CatalogRepository,
    private val datasetRepository: DatasetRepository,
    private val turtleService: TurtleService
) {

    fun updateUnionModels() {
        var catalogUnion = ModelFactory.createDefaultModel()
        var noRecordsUnion = ModelFactory.createDefaultModel()

        catalogRepository.findAll()
            .forEach {
                turtleService.getCatalog(it.fdkId, withRecords = true)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { catalogUnion = catalogUnion.union(this) }

                turtleService.getCatalog(it.fdkId, withRecords = false)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { noRecordsUnion = noRecordsUnion.union(this) }
            }

        turtleService.saveAsCatalogUnion(catalogUnion, true)
        turtleService.saveAsCatalogUnion(noRecordsUnion, false)
    }

    fun updateMetaData() {
        datasetRepository.findAll()
            .forEach { dataset ->
                val datasetMeta = dataset.createMetaModel()

                turtleService.getDataset(dataset.fdkId, withRecords = false)
                    ?.let { conceptNoRecords -> parseRDFResponse(conceptNoRecords, Lang.TURTLE, null) }
                    ?.let { conceptModelNoRecords -> datasetMeta.union(conceptModelNoRecords) }
                    ?.run { turtleService.saveAsDataset(this, fdkId = dataset.fdkId, withRecords = true) }
            }

        catalogRepository.findAll()
            .forEach { catalog ->
                val catalogNoRecords = turtleService.getCatalog(catalog.fdkId, withRecords = false)
                    ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

                if (catalogNoRecords != null) {
                    val catalogURI = "${applicationProperties.catalogUri}/${catalog.fdkId}"
                    var catalogMeta = catalog.createMetaModel()

                    datasetRepository.findAllByIsPartOf(catalogURI)
                        .filter {
                            it.modelContainsDataset(catalogNoRecords)
                        }
                        .forEach { dataService ->
                            val serviceMetaModel = dataService.createMetaModel()
                            catalogMeta = catalogMeta.union(serviceMetaModel)
                        }

                    turtleService.saveAsCatalog(
                        catalogMeta.union(catalogNoRecords),
                        fdkId = catalog.fdkId,
                        withRecords = true
                    )
                }
            }

        updateUnionModels()
    }

    private fun CatalogMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.catalogUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()
        metaModel.addMetaPrefixes()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun DatasetMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.datasetUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()
        metaModel.addMetaPrefixes()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.isPartOf, metaModel.createResource(isPartOf))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun DatasetMeta.modelContainsDataset(model: Model): Boolean =
        model.containsTriple("<${uri}>", "a", "<${DCAT.Dataset.uri}>")
            || model.containsTriple("<${uri}>", "a", "<${DCAT3.DatasetSeries.uri}>")

}
