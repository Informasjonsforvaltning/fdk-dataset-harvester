package no.dcat.datastore.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by havardottestad on 18/01/16.
 */
@Tag("unit")
public class HarvestTest {

    @Test
    public void testGetCreatedDateFormatted() throws Exception {

        DcatSource.Harvest harvest = new DcatSource().new Harvest(null, "2016-01-12T10:00:00.00+00:00", null);
        String createdDateFormatted = harvest.getCreatedDateFormatted();

        assertEquals("12.01.16 11:00", createdDateFormatted, "Should give out datetime in oslo timezone without the timezone ending");

    }
}
