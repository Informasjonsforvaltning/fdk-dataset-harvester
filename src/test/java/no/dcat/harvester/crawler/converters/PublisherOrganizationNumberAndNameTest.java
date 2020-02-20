package no.dcat.harvester.crawler.converters;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
public class PublisherOrganizationNumberAndNameTest {
    Logger logger = LoggerFactory.getLogger(PublisherOrganizationNumberAndNameTest.class);

    String expected = "974760983";

    @Test
    public void testExtractOrgnrFromURI() throws Throwable {
        String actual = EnhetsregisterResolver.getOrgNrFromUri("http://data.brreg.no/enhetsregisteret/enhet/974760983");

        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void testExtractOrgnrFromURIWithXml() {
        String actual = EnhetsregisterResolver.getOrgNrFromUri("http://data.brreg.no/enhetsregisteret/enhet/974760983.xml");

        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void testExtractOrgnrFromURIWithjson() {
        String actual = EnhetsregisterResolver.getOrgNrFromUri("http://data.brreg.no/enhetsregisteret/enhet/974760983.json");

        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void testExtractOrgnrFromLongUri() {
        String actual = EnhetsregisterResolver.getOrgNrFromUri("http://data.brreg.no/enhetsregisteret/enhet/974760983/fovar.xml");

        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void testExtractOrgnrOnlyFindsFirstNumber() {
        String actual = EnhetsregisterResolver.getOrgNrFromUri("http://data.brreg.no/enhetsregisteret/enhet/974760983/fovar32.xml");

        Assert.assertThat(actual, Is.is(expected));
    }


}
