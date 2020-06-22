package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property

class DCATAPI {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://dcat.no/dcatapi/"
        val accessService: Property = m.createProperty( "${uri}accessService")
        val endpointDescription: Property = m.createProperty("${uri}endpointDescription")
    }
}

class DCATNO {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://difi.no/dcatno#"
        val informationModel: Property = m.createProperty("${uri}informationModel")
    }
}

class ADMS {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://www.w3.org/ns/adms#"
        val sample: Property = m.createProperty("${uri}sample")
    }
}

class DQV {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://www.w3.org/ns/dqvNS#"
        val hasQualityAnnotation: Property = m.createProperty("${uri}hasQualityAnnotation")
    }
}

class PROV {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://www.w3.org/ns/prov#"
        val hasBody: Property = m.createProperty("${uri}hasBody")
    }
}
