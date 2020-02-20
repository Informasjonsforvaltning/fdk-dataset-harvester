package no.dcat.datastore;

import java.util.Arrays;
import java.util.Optional;

import no.dcat.datastore.domain.DcatSource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class HarvestTest {
	
	@Test
	public void testHarvestComparator() {
		
		DcatSource.Harvest h1 = new DcatSource().new Harvest(ResourceFactory.createResource("http://dcat.difi.no/dcatSource_h1"), "2014-01-01T12:00:00.000+00:00", "h1");
		DcatSource.Harvest h2 = new DcatSource().new Harvest(ResourceFactory.createResource("http://dcat.difi.no/dcatSource_h2"), "2015-01-01T12:00:00.000+00:00", "h2");
		DcatSource.Harvest h3 = new DcatSource().new Harvest(ResourceFactory.createResource("http://dcat.difi.no/dcatSource_h3"), "2016-01-01T12:00:00.000+00:00", "h3");

		DcatSource dcatSource = new DcatSource();
		dcatSource.getHarvested().addAll(Arrays.asList(h1, h3, h2));

		Optional<DcatSource.Harvest> harvest = dcatSource.getLastHarvest();

		assertTrue(harvest.isPresent(), "Expected harvest to be present");
		assertEquals(h3, harvest.get(), "Expected \"h3\" to be the latest harvest");
	}
}
