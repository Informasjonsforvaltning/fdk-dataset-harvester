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

fun Model.createRDFResponse(): String =
    ByteArrayOutputStream().use{ out ->
        write(out, "TURTLE")
        out.flush()
        out.toString("UTF-8")
    }
