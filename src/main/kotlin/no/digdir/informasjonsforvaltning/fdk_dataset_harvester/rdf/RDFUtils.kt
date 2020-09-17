package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.Application
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
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.*

private val logger = LoggerFactory.getLogger(Application::class.java)
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

fun parseRDFResponse(responseBody: String, rdfLanguage: JenaType, rdfSource: String?): Model? {
    val responseModel = ModelFactory.createDefaultModel()

    try {
        responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.value)
    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed: ${ex.message}")
        return null
    }

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

fun Resource.modelOfResourceProperties(property: Property): Model {
    val model = ModelFactory.createDefaultModel()

    listProperties(property)
        .toList()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .forEach { model.add(it.listProperties()) }

    return model
}

fun Resource.modelOfDistributionProperties(): Model {
    val model = ModelFactory.createDefaultModel()

    listProperties(DCAT.distribution)
        .toList()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .forEach {
            model.add(it.listProperties())
            it.listProperties().toList()
                .filter { property -> property.isResourceProperty() }
                .forEach { property ->
                    if (property.predicate == DCATAPI.accessService) {
                        model.add(property.resource.listProperties())
                        property.resource.listProperties(DCATAPI.endpointDescription).toList()
                            .filter { endpoint -> endpoint.isResourceProperty() }
                            .forEach { endpoint -> model.add(endpoint.resource.listProperties()) }
                    } else model.add(property.resource.listProperties())
                }
        }

    return model
}

fun Resource.modelOfQualityProperties(): Model {
    val model = ModelFactory.createDefaultModel()

    listProperties(DQV.hasQualityAnnotation)
        .toList()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .forEach {
            model.add(it.listProperties())
            it.listProperties().toList()
                .filter { body -> body.isResourceProperty() }
                .forEach { body -> model.add(body.resource.listProperties()) }
        }

    return model
}

fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun createIdFromUri(uri: String): String =
    UUID.nameUUIDFromBytes(uri.toByteArray())
        .toString()
