package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VCARD4
import org.apache.jena.vocabulary.XSD
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.*

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
    responseModel.read(StringReader(responseBody), "", rdfLanguage.value)
    return responseModel
}

fun Model.createRDFResponse(responseType: JenaType): String =
    ByteArrayOutputStream().use{ out ->
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

    return this
}

fun Resource.createModelOfTopLevelProperties(): Model {
    val newModel = ModelFactory.createDefaultModel()
    newModel.add(listProperties())

    return newModel
}

fun Resource.createDatasetModel(): Model {
    val newModel = ModelFactory.createDefaultModel()
    newModel.add(listProperties())

    extractProperty(DCAT.contactPoint)
        ?.run { newModel.add(this.resource.listProperties()) }

    extractProperty(DCTerms.temporal)
        ?.run { newModel.add(this.resource.listProperties()) }

    val distributionModels = mutableListOf<Model>()

    listProperties(DCAT.distribution).toList()
        .forEach {statement ->
            statement.resource.uri
                .let {uri -> model.getResource(uri) }
                .createModelOfTopLevelProperties()
                .run { distributionModels.add(this) }
        }

    return newModel.union(distributionModels.unionModelOfList())
}

private fun List<Model>.unionModelOfList(): Model {
    var unionModel = ModelFactory.createDefaultModel()
    forEach { unionModel = unionModel.union(it) }

    return unionModel
}

private fun Resource.extractProperty(property: Property) : Statement? =
    if (this.hasProperty(property)) this.getProperty(property)
    else null

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
