package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.adapter.HarvestAdminAdapter
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.ApplicationProperties
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestAdminParameters
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals

@Tag("unit")
class HarvestAdminAdapterTest {
    private val valuesMock: ApplicationProperties = mock()
    private val adapter = HarvestAdminAdapter(valuesMock)

    @Nested
    internal inner class UrlWithParameters {

        @Test
        fun nullParameters() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(HarvestAdminParameters(null, null, null, null))

            assertEquals(URL("http://www.example.com/datasources"), url)
        }

        @Test
        fun emptyParameters() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(HarvestAdminParameters("", "", "", ""))

            assertEquals(URL("http://www.example.com/datasources"), url)
        }

        @Test
        fun defaultParameter() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(HarvestAdminParameters())

            assertEquals(URL("http://www.example.com/datasources?dataType=dataset"), url)
        }

        @Test
        fun publisherIdAndDataSourceId() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(
                HarvestAdminParameters(
                    publisherId = "123456789",
                    dataSourceId = "ds-id"
                )
            )

            assertEquals(URL("http://www.example.com/organizations/123456789/datasources/ds-id?dataType=dataset"), url)
        }

        @Test
        fun severalParameters() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(
                HarvestAdminParameters(
                    publisherId = "123456789",
                    dataSourceType = "DCAT-AP-NO",
                    dataType = "dataset"
                )
            )

            assertEquals(URL("http://www.example.com/organizations/123456789/datasources?dataType=dataset&dataSourceType=DCAT-AP-NO"), url)
        }

    }
}