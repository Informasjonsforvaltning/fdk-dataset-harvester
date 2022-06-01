package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class DCAT3 {

    companion object {
        const val uri = "http://www.w3.org/ns/dcat#"

        val DatasetSeries: Resource = ResourceFactory.createResource("${uri}DatasetSeries")
        val inSeries: Property = ResourceFactory.createProperty("${uri}inSeries")
    }

}
