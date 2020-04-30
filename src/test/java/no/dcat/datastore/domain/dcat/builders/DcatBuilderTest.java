package no.dcat.datastore.domain.dcat.builders;

import no.dcat.datastore.domain.dcat.DcatConverterTest;
import no.dcat.shared.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by bjg on 20.03.2019.
 */
@Tag("unit")
public class DcatBuilderTest {
    private static Logger logger = LoggerFactory.getLogger(DcatConverterTest.class);

    public DcatReader setupReader(Model model) {
        return new DcatReader(model);
    }

    @Test
    public void writeDistributionOK() throws Throwable {

        Model model = RDFDataMgr.loadModel("dataset-with-distribution.ttl");

        String expectedDistribution = "dcat:distribution  [ a                dcat:Distribution ;\n" +
            "                             dct:description  \"En vanlig distribusjon\"@nb ;\n" +
            "                             dct:format       \"application/json\" ;\n" +
            "                             dct:license      [ a           dct:LicenseDocument , skos:Concept ;\n" +
            "                                                dct:source  \"http://data.norge.no/nlod/\"\n" +
            "                                              ] ;\n" +
            "                             dct:type         \"Nedlastbar fil\" ;\n" +
            "                             dcat:accessURL   <http://www.brreg.no>\n" +
            "                           ] .";

        DcatReader reader = setupReader(model);
        Dataset dataset = reader.getDatasets().get(0);

        DcatBuilder dcatBuilder = new DcatBuilder();
        String rdf = dcatBuilder.transform(dataset, "TURTLE");

        assertThat(rdf.contains(expectedDistribution), is(true));

    }


    @Test
    public void writeApiDistributionOK() throws Throwable {

        Model model = RDFDataMgr.loadModel("dataset-with-api-distribution.ttl");


        DcatReader reader = setupReader(model);
        Dataset dataset = reader.getDatasets().get(0);
        String endpointDescriptionUri = dataset.getDistribution().get(0).getAccessService().get(0).getUri();

        DcatBuilder dcatBuilder = new DcatBuilder();
        String rdf = dcatBuilder.transform(dataset, "TURTLE");


        String expectedAccessService = "dcatapi:accessService  <" + endpointDescriptionUri + ">";


        String expectedEndpointDescription = "<" + endpointDescriptionUri + ">\n" +
            "        a                            dcatapi:DataDistributionService ;\n" +
            "        dcatapi:endpointDescription  [ a           foaf:Document , skos:Concept ;\n" +
            "                                       dct:source  \"93b125a8-c68c-4d65-8b16-6b45b9453be4\"\n" +
            "                                     ] ;\n" +
            "        dct:description              \"Åpne Data fra Enhetsregisteret - API Dokumentasjon\"@nb .";

        assertThat(rdf.contains(expectedAccessService), is(true));
        assertThat(rdf.contains(expectedEndpointDescription), is(true));

    }

}
