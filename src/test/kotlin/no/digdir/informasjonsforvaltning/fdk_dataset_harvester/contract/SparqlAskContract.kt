package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.contract

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.ApiTestContext
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        properties = ["spring.profiles.active=contract-test"],
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class SparqlAskContract : ApiTestContext() {

    @Test
    fun testShouldReturn200forQueriesWithResult() {}
    @Test
    fun testShouldReturn204forQueriesWithNoResult() {}
    @Test
    fun testShouldReturn400forMalformedSparqlQueries() {}
    @Test
    fun testShouldReturn403forQueriesContainingUpdate() {}

}