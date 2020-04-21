package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property

class DCATAPI {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://dcat.no/dcatapi/"
        val accessService: Property = m.createProperty(uri + "accessService")
    }
}