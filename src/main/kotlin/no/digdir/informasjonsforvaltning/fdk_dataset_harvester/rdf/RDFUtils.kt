package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.Application
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
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
        accept.contains(Lang.TURTLE.headerString) -> Lang.TURTLE
        accept.contains("text/n3") -> Lang.N3
        accept.contains(Lang.TRIG.headerString) -> Lang.TRIG
        accept.contains(Lang.RDFXML.headerString) -> Lang.RDFXML
        accept.contains(Lang.RDFJSON.headerString) -> Lang.RDFJSON
        accept.contains(Lang.JSONLD.headerString) -> Lang.JSONLD
        accept.contains(Lang.NTRIPLES.headerString) -> Lang.NTRIPLES
        accept.contains(Lang.NQUADS.headerString) -> Lang.NQUADS
        accept.contains(Lang.TRIX.headerString) -> Lang.TRIX
        accept.contains("*/*") -> null
        else -> Lang.RDFNULL
    }

fun parseRDF(responseBody: String, rdfLanguage: Lang): Model {
    val responseModel = ModelFactory.createDefaultModel()
    responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.name)

    // test that the model is valid as RDF/XML, will throw exception if not
    responseModel.createRDFResponse(Lang.RDFXML)

    return responseModel
}

fun safeParseRDF(rdf: String, lang: Lang): Model =
    try {
        ModelFactory.createDefaultModel()
            .read(StringReader(rdf), BACKUP_BASE_URI, lang.name)
    } catch (ex: Exception) {
        logger.warn("parse failure", ex)
        ModelFactory.createDefaultModel()
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

fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun createIdFromString(idBase: String): String =
    UUID.nameUUIDFromBytes(idBase.toByteArray())
        .toString()


fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

fun Model.containsTriple(subj: String, pred: String, obj: String): Boolean {
    val askQuery = "ASK { $subj $pred $obj }"

    return try {
        val query = QueryFactory.create(askQuery)
        QueryExecutionFactory.create(query, this).execAsk()
    } catch (ex: Exception) { false }
}
