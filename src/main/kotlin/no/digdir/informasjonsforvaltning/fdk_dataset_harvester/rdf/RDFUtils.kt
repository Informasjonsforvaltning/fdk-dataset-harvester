package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.Application
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
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

fun jenaTypeFromAcceptHeader(accept: String?): Lang? =
    when {
        accept == null -> null
        accept.contains("text/turtle") -> Lang.TURTLE
        accept.contains("application/rdf+xml") -> Lang.RDFXML
        accept.contains("application/rdf+json") -> Lang.RDFJSON
        accept.contains("application/ld+json") -> Lang.JSONLD
        accept.contains("application/n-triples") -> Lang.NTRIPLES
        accept.contains("text/n3") -> Lang.N3
        accept.contains("*/*") -> null
        else -> Lang.RDFNULL
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: Lang, rdfSource: String?): Model? {
    val responseModel = ModelFactory.createDefaultModel()

    try {
        responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.name)
    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed: ${ex.message}")
        return null
    }

    return responseModel
}

fun Model.createRDFResponse(responseType: Lang): String =
    ByteArrayOutputStream().use { out ->
        write(out, responseType.name)
        out.flush()
        out.toString("UTF-8")
    }

fun Model.addMetaPrefixes(): Model {
    setNsPrefix("dct", DCTerms.NS)
    setNsPrefix("dcat", DCAT.NS)
    setNsPrefix("foaf", FOAF.getURI())
    setNsPrefix("xsd", XSD.NS)

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


fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

fun Model.containsTriple(subj: String, pred: String, obj: String): Boolean {
    val askQuery = "ASK { $subj $pred $obj }"

    val query = QueryFactory.create(askQuery)
    return QueryExecutionFactory.create(query, this).execAsk()
}
