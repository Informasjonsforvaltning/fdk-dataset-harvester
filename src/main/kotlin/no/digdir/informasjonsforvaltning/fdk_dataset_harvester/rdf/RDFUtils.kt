package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.apache.jena.vocabulary.VCARD4
import org.apache.jena.vocabulary.XSD
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.*

const val BACKUP_BASE_URI = "http://example.com/"

enum class JenaType(val value: String){
    TURTLE("TURTLE"),
    RDF_XML("RDF/XML"),
    RDF_JSON("RDF/JSON"),
    JSON_LD("JSON-LD"),
    NTRIPLES("N-TRIPLES"),
    N3("N3"),
    NOT_ACCEPTABLE("NOT ACCEPTABLE")
}

fun jenaTypeFromAcceptHeader(accept: String?): JenaType? =
    when (accept) {
        "text/turtle" -> JenaType.TURTLE
        "application/rdf+xml" -> JenaType.RDF_XML
        "application/rdf+json" -> JenaType.RDF_JSON
        "application/ld+json" -> JenaType.JSON_LD
        "application/n-triples" -> JenaType.NTRIPLES
        "text/n3" -> JenaType.N3
        "*/*" -> null
        null -> null
        else -> JenaType.NOT_ACCEPTABLE
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: JenaType): Model {
    val responseModel = ModelFactory.createDefaultModel()
    responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.value)
    return responseModel
}

fun Model.createRDFResponse(responseType: JenaType): String =
    ByteArrayOutputStream().use { out ->
        write(out, responseType.value)
        out.flush()
        out.toString("UTF-8")
    }

fun Model.addDefaultPrefixes(): Model {
    setNsPrefix("dct", DCTerms.NS)
    setNsPrefix("dcat", DCAT.NS)
    setNsPrefix("foaf", FOAF.getURI())
    setNsPrefix("vcard", VCARD4.NS)
    setNsPrefix("xsd", XSD.NS)
    setNsPrefix("skos", SKOS.uri)
    setNsPrefix("rdf", RDF.uri)
    setNsPrefix("adms", "http://www.w3.org/ns/adms#")
    setNsPrefix("dcatno", "http://difi.no/dcatno#")
    setNsPrefix("dqv", "http://www.w3.org/ns/dqvNS#")
    setNsPrefix("prov", "http://www.w3.org/ns/prov#")
    setNsPrefix("dcatapi", DCATAPI.uri)

    return this
}

fun Resource.createDatasetModel(): Model =
    listProperties()
        .toModel()
        .addNonURIPropertiesFromResource(this)
        .addURIResourceProperties(this, DCTerms.publisher)
        .addURIResourceProperties(this, DCAT.contactPoint)
        .addURIResourceProperties(this, DCTerms.temporal)
        .addURIResourceProperties(this, DCAT.distribution)
        .addURIResourceDistributionAccessService(this)

fun Resource.createModel(): Model =
    listProperties()
        .toModel()
        .addNonURIPropertiesFromResource(this)

private fun Model.addNonURIPropertiesFromResource(resource: Resource): Model {
    add(resource.listProperties())

    resource.listProperties()
        .toList()
        .filter { it.isResourceProperty() }
        .filter { !it.resource.isURIResource }
        .forEach { addNonURIPropertiesFromResource(it.resource) }

    return this
}

private fun Model.addURIResourceProperties(resource: Resource, property: Property): Model {
    resource.listProperties(property)
        .toList()
        .filter { it.isResourceProperty() && it.resource.isURIResource }
        .forEach { addNonURIPropertiesFromResource(it.resource) }

    return this
}

private fun Model.addURIResourceDistributionAccessService(resource: Resource): Model {
    resource.listProperties(DCAT.distribution)
        .forEach { distribution ->
            distribution.resource
                .listProperties(DCATAPI.accessService)
                .toList()
                .filter { accessService ->
                    accessService.isResourceProperty() && accessService.resource.isURIResource }
                .forEach { accessService ->
                    addNonURIPropertiesFromResource(accessService.resource)
                }
        }

    return this
}

private fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun Model.extractMetaDataIdentifier(): String =
    listResourcesWithProperty(RDF.type, DCAT.record)
        .toList()
        .first()
        .getProperty(DCTerms.identifier).string

fun createIdFromUri(uri: String): String =
    UUID.nameUUIDFromBytes(uri.toByteArray())
        .toString()

fun Model.extractCatalogModelURI(): String =
    listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .first()
        .uri

fun Model.extractDatasetModelURI(): String =
    listResourcesWithProperty(RDF.type, DCAT.Dataset)
        .toList()
        .first()
        .uri
