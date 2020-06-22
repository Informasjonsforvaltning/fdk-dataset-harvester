package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.TestResponseReader
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class DatasetProperties {
    private val responseReader = TestResponseReader()

    @Test
    fun listChangedDatasets() {
        val catalog0 = responseReader.parseFile("catalog_diff_0.ttl", "TURTLE")
        val catalog1 = responseReader.parseFile("catalog_diff_1.ttl", "TURTLE")

        val changed = changedCatalogAndDatasets(catalog0, catalog1)

        val expected = mapOf(Pair("https://testdirektoratet.no/model/dataset-catalog/0", listOf(
                "https://testdirektoratet.no/model/dataset/1", "https://testdirektoratet.no/model/dataset/3",
                "https://testdirektoratet.no/model/dataset/5", "https://testdirektoratet.no/model/dataset/7",
                "https://testdirektoratet.no/model/dataset/9", "https://testdirektoratet.no/model/dataset/b",
                "https://testdirektoratet.no/model/dataset/d")))

        assertEquals(expected["https://testdirektoratet.no/model/dataset-catalog/0"], changed["https://testdirektoratet.no/model/dataset-catalog/0"]?.sorted())
    }

    @Nested
    internal inner class ModelDifferences {

        @Test
        fun datasetsAreEqual() {
            val catalog0 = responseReader.parseFile("catalog_diff_0.ttl", "TURTLE")
            val duplicate0 = responseReader.parseFile("catalog_diff_1.ttl", "TURTLE")

            assertFalse { datasetDiffersInModels("https://testdirektoratet.no/model/dataset/0", catalog0, duplicate0) }
        }

        @Test
        fun missingDatasetRegistersAsDifference() {
            val catalogModel = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")
            val allCatalogsModel = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

            assertTrue { datasetDiffersInModels("https://testdirektoratet.no/model/dataset/1", allCatalogsModel, catalogModel) }
            assertFalse { datasetDiffersInModels("https://testdirektoratet.no/model/dataset/0", allCatalogsModel, catalogModel) }
        }

        @Test
        fun datasetLiteralDifference() {
            val model0 = responseReader.parseResponse("""
                @prefix dcat:  <http://www.w3.org/ns/dcat#> .
                @prefix dct:   <http://purl.org/dc/terms/> .

                <https://testdirektoratet.no/model/dataset/0>
                    a                         dcat:Dataset ;
                    dct:title                 "Dataset 0"@nb .""".trimIndent(), "TURTLE")
            val model1 = responseReader.parseResponse("""
                @prefix dcat:  <http://www.w3.org/ns/dcat#> .
                @prefix dct:   <http://purl.org/dc/terms/> .

                <https://testdirektoratet.no/model/dataset/0>
                    a                         dcat:Dataset ;
                    dct:title                 "Different title"@nb .""".trimIndent(), "TURTLE")

            assertTrue { datasetDiffersInModels("https://testdirektoratet.no/model/dataset/0", model0, model1) }
        }

        @Test
        fun datasetBNodeDifference() {
            val model0 = responseReader.parseResponse("""
                @prefix dcat:  <http://www.w3.org/ns/dcat#> .
                @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .

                <https://testdirektoratet.no/model/dataset/0>
                    a                         dcat:Dataset ;
                    dcat:contactPoint         [ 
                        a                          vcard:Organization ;
                        vcard:hasOrganizationName  "Testdirektoratet"@nb ;
                        vcard:hasURL               <https://testdirektoratet.no> ] .""".trimIndent(), "TURTLE")
            val model1 = responseReader.parseResponse("""
                @prefix dcat:  <http://www.w3.org/ns/dcat#> .
                @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .

                <https://testdirektoratet.no/model/dataset/0>
                    a                         dcat:Dataset ;
                    dcat:contactPoint         [ 
                        a                          vcard:Organization ;
                        vcard:hasOrganizationName  "Testdirektoratet"@nb ;
                        vcard:hasURL               <https://testdirektorat.no> ] .""".trimIndent(), "TURTLE")

            assertTrue { datasetDiffersInModels("https://testdirektoratet.no/model/dataset/0", model0, model1) }
        }

        @Test
        fun datasetURINodeDifference() {
            val model0 = responseReader.parseResponse("""
                @prefix dcat:  <http://www.w3.org/ns/dcat#> .
                @prefix dct:   <http://purl.org/dc/terms/> .
                @prefix foaf:  <http://xmlns.com/foaf/0.1/> .

                <https://testdirektoratet.no/model/dataset/0>
                    a               dcat:Dataset ;
                    dct:publisher   <https://data.brreg.no/enhetsregisteret/api/enheter/123456789> .
                    
                <https://data.brreg.no/enhetsregisteret/api/enheter/123456789>
                    a               foaf:Agent ;
                    dct:identifier  "123456789" ;
                    foaf:name       "testorg" .""".trimIndent(), "TURTLE")
            val model1 = responseReader.parseResponse("""
                @prefix dcat:  <http://www.w3.org/ns/dcat#> .
                @prefix dct:   <http://purl.org/dc/terms/> .
                @prefix foaf:  <http://xmlns.com/foaf/0.1/> .

                <https://testdirektoratet.no/model/dataset/0>
                    a               dcat:Dataset ;
                    dct:publisher   <https://data.brreg.no/enhetsregisteret/api/enheter/123456789> .
                    
                <https://data.brreg.no/enhetsregisteret/api/enheter/123456789>
                    a               foaf:Agent ;
                    dct:identifier  "123456789" ;
                    foaf:name       "Testorg" .""".trimIndent(), "TURTLE")

            assertTrue { datasetDiffersInModels("https://testdirektoratet.no/model/dataset/0", model0, model1) }
        }

    }
}