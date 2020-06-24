package no.dcat.datastore.domain.dcat.builders;

import no.dcat.datastore.domain.dcat.Distribution;
import no.dcat.shared.SkosCode;
import no.dcat.shared.SkosConcept;
import no.dcat.shared.DataDistributionService;
import no.dcat.datastore.domain.dcat.vocabulary.DCAT;
import no.dcat.datastore.domain.dcat.vocabulary.DCATapi;
import no.dcat.shared.Types;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class DistributionBuilder extends AbstractBuilder {
    private static Logger logger = LoggerFactory.getLogger(DistributionBuilder.class);

    protected final Model model;
    protected final Map<String, Map<String, SkosCode>> codes;


    public DistributionBuilder(Model model, Map<String, Map<String, SkosCode>> codes) {
        this.model = model;
        this.codes = codes;
    }

    public List<Distribution> build() {
        List<Distribution> distributions = new ArrayList<>();

        ResIterator catalogIterator = model.listResourcesWithProperty(RDF.type, DCAT.Catalog);
        while (catalogIterator.hasNext()) {
            Resource catalog = catalogIterator.next();

            StmtIterator datasetIterator = catalog.listProperties(DCAT.dataset);

            while (datasetIterator.hasNext()) {
                Resource dataset = datasetIterator.next().getResource();
                StmtIterator distributionIterator = dataset.listProperties(DCAT.distribution);

                while (distributionIterator.hasNext()) {
                    Statement next = distributionIterator.nextStatement();

                    if (next.getObject().isResource()) {
                        Resource distribution = next.getResource();
                        distributions.add(create(distribution, codes));
                    }
                }
            }
        }

        return distributions;
    }

    // Helper method used to compare substrings of two strings starting with first forward slash, intended to be used to compare if URLS are equal when disregarding protocol.
    static boolean compareURLs(String distributionURL, String openURL) {
        if (distributionURL == null || openURL == null) {
            return false;
        }
        distributionURL = distributionURL.replaceFirst("^.*//", "");
        openURL = openURL.replaceFirst("^.*//", "");
        return distributionURL.startsWith(openURL);
    }

    public static Distribution create(Resource distResource,
                                      Map<String, Map<String, SkosCode>> codes) {
        Distribution dist = new Distribution();

        if (distResource != null) {
            dist.setId(null);
            dist.setUri(distResource.getURI());

            dist.setTitle(extractLanguageLiteral(distResource, DCTerms.title));
            dist.setDescription(extractLanguageLiteral(distResource, DCTerms.description));
            dist.setAccessURL(extractUriList(distResource, DCAT.accessUrl));
            dist.setDownloadURL(extractUriList(distResource, DCAT.downloadUrl));
            dist.setOpenLicense(false);

            List<SkosConcept> licenses = extractSkosConcept(distResource, DCTerms.license);

            if (licenses != null && licenses.size() > 0) {
                AtomicReference<SkosConcept> chosenLicense = new AtomicReference<>(licenses.get(0));
                AtomicBoolean licenseIsOpen = new AtomicBoolean(false);

                // can we add a prefLabel on a uri with an open license
                if (codes != null) {
                    Map<String, SkosCode> licenseCodeMap = codes.get(Types.openlicenses.getType());
                    if (licenseCodeMap != null) {
                        licenses.forEach(license -> {
                            licenseCodeMap.forEach((key, code) -> {
                                if(compareURLs(license.getUri(), code.getUri())) {
                                    if (license.getPrefLabel() == null) {
                                        license.setPrefLabel(code.getPrefLabel());
                                    }
                                    // open licenses are prioritized
                                    chosenLicense.set(license);
                                    licenseIsOpen.set(true);
                                }
                            });
                        });
                    }
                }

                dist.setLicense(chosenLicense.get());
                dist.setOpenLicense(licenseIsOpen.get());

                if (licenses.size() > 1) {
                    logger.warn("There are more than one recommended license in input data. Only one will be kept");
                }
            };

            dist.setConformsTo(extractSkosConcept(distResource, DCTerms.conformsTo));
            dist.setPage(extractSkosConcept(distResource, FOAF.page));
            dist.setFormat(extractMultipleStringsExcludeBaseUri(distResource, DCTerms.format));


            dist.setType(extractAsString(distResource, DCTerms.type));

            dist.setAccessService(getDataDistributionServices(distResource));

        }

        return dist;
    }


    public static List<DataDistributionService> getDataDistributionServices(Resource distResource) {
        return distResource
                .listProperties(DCATapi.accessService)
                .toList()
                .stream()
                .map(Statement::getObject)
                .filter(RDFNode::isResource)
                .map(RDFNode::asResource)
                .map(DataDistributionServiceBuilder::create)
                .collect(Collectors.toList());
    }

}