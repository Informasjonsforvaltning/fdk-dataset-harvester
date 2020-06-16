package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

val META_CATALOG_0 = """
<http://localhost:5000/catalogs/6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f>
        a       <http://www.w3.org/ns/dcat#CatalogRecord> ;
        <http://purl.org/dc/terms/identifier>
                "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f" ;
        <http://purl.org/dc/terms/issued>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://purl.org/dc/terms/modified>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://xmlns.com/foaf/0.1/primaryTopic>
                <https://testdirektoratet.no/model/dataset-catalog/0> .
""".trimIndent()

val META_CATALOG_1 = """
<http://localhost:5000/catalogs/6f0a37af-a9c1-38bc-b343-bd025b43b5e8>
        a       <http://www.w3.org/ns/dcat#CatalogRecord> ;
        <http://purl.org/dc/terms/identifier>
                "6f0a37af-a9c1-38bc-b343-bd025b43b5e8" ;
        <http://purl.org/dc/terms/issued>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://purl.org/dc/terms/modified>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://xmlns.com/foaf/0.1/primaryTopic>
                <https://testdirektoratet.no/model/dataset-catalog/1> .
""".trim()

val META_DATASET_0 ="""
<http://localhost:5000/datasets/a1c680ca-62d7-34d5-aa4c-d39b5db033ae>
        a       <http://www.w3.org/ns/dcat#CatalogRecord> ;
        <http://purl.org/dc/terms/identifier>
                "a1c680ca-62d7-34d5-aa4c-d39b5db033ae" ;
        <http://purl.org/dc/terms/issued>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://purl.org/dc/terms/modified>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://xmlns.com/foaf/0.1/primaryTopic>
                <https://testdirektoratet.no/model/dataset/0> .
""".trim()

val META_DATASET_1 ="""
<http://localhost:5000/datasets/4667277a-9d27-32c1-aed5-612fa601f393>
        a       <http://www.w3.org/ns/dcat#CatalogRecord> ;
        <http://purl.org/dc/terms/identifier>
                "4667277a-9d27-32c1-aed5-612fa601f393" ;
        <http://purl.org/dc/terms/issued>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://purl.org/dc/terms/modified>
                "2020-03-12T11:52:16.122Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://xmlns.com/foaf/0.1/primaryTopic>
                <https://testdirektoratet.no/model/dataset/1> .
""".trim()

val HARVEST_0 = """
<https://testdirektoratet.no/model/dataset-catalog/0>
        a       <http://www.w3.org/ns/dcat#Catalog> ;
        <http://purl.org/dc/terms/publisher>
                <https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789> ;
        <http://purl.org/dc/terms/title>
                "Datasettkatalog for Testdirektoratet"@nb ;
        <http://www.w3.org/ns/dcat#dataset>
                <https://testdirektoratet.no/model/dataset/0> .

<https://testdirektoratet.no/model/dataset/0>
        a       <http://www.w3.org/ns/dcat#Dataset> ;
        <http://purl.org/dc/terms/accessRights>
                <http://publications.europa.eu/resource/authority/access-right/PUBLIC> ;
        <http://purl.org/dc/terms/description>
                "Description of dataset 0"@nb ;
        <http://purl.org/dc/terms/identifier>
                "adb4cf00-31c8-460c-9563-55f204cf8221" ;
        <http://purl.org/dc/terms/issued>
                "2019-03-22T13:11:16.546902"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://purl.org/dc/terms/language>
                <http://publications.europa.eu/resource/authority/language/NOR> ;
        <http://purl.org/dc/terms/temporal>
                [ a       <http://purl.org/dc/terms/PeriodOfTime> ;
                  <http://www.w3.org/ns/dcat#startDate>
                          "2019-04-02T00:00:00"^^<http://www.w3.org/2001/XMLSchema#dateTime>
                ] ;
        <http://purl.org/dc/terms/title>
                "Dataset 0"@nb ;
        <http://www.w3.org/ns/dcat#contactPoint>
                [ a       <http://www.w3.org/2006/vcard/ns#Organization> ;
                  <http://www.w3.org/2006/vcard/ns#hasOrganizationName>
                          "Testdirektoratet"@nb ;
                  <http://www.w3.org/2006/vcard/ns#hasURL>
                          <https://testdirektoratet.no>
                ] ;
        <http://www.w3.org/ns/dcat#distribution>
                <https://testdirektoratet.no/model/distribution/0> ;
        <http://www.w3.org/ns/dcat#endpointDescription>
                <https://testdirektoratet.no/openapi/dataset/0.yaml> ;
        <http://www.w3.org/ns/dcat#keyword>
                "fest" , "test" ;
        <http://www.w3.org/ns/dcat#theme>
                <http://publications.europa.eu/resource/authority/data-theme/TECH> , <http://publications.europa.eu/resource/authority/data-theme/GOVE> ;
        <http://xmlns.com/foaf/0.1/page>
                <https://testdirektoratet.no> .

<https://testdirektoratet.no/model/distribution/0>
        a       <http://www.w3.org/ns/dcat#Distribution> ;
        <http://purl.org/dc/terms/format>
                "CSV JSON JSONP YAML XML" ;
        <http://purl.org/dc/terms/license>
                <https://data.norge.no/nlod/no> ;
        <http://purl.org/dc/terms/title>
                "Test distribution" ;
        <http://www.w3.org/ns/dcat#accessURL>
                <http://testdirektoratet.no/data/test/fest> ;
        <http://dcat.no/dcatapi/accessService>
                <http://example.com/92b5e4f7-dbbd-482c-b242-990a3628d395> .

<http://example.com/92b5e4f7-dbbd-482c-b242-990a3628d395>
        a       <http://dcat.no/dcatapi/DataDistributionService> ;
        <http://dcat.no/dcatapi/endpointDescription>   
                [ a             <http://xmlns.com/foaf/0.1/Document> , <http://www.w3.org/2004/02/skos/core#Concept> ;
                  <http://purl.org/dc/terms/source>  "84caab4b-b004-4a97-9b92-7bf335cf50d2"
                ] ;
        <http://purl.org/dc/terms/description>
                "Search API"@nb .
""".trim()

val HARVEST_1 = """
<https://testdirektoratet.no/model/dataset-catalog/1>
        a       <http://www.w3.org/ns/dcat#Catalog> ;
        <http://purl.org/dc/terms/publisher>
                <https://organization-catalogue.fellesdatakatalog.brreg.no/organizations/123456789> ;
        <http://purl.org/dc/terms/title>
                "Datasettkatalog 1 for Testdirektoratet"@nb ;
        <http://www.w3.org/ns/dcat#dataset>
                <https://testdirektoratet.no/model/dataset/1> .

<https://testdirektoratet.no/model/dataset/1>
        a       <http://www.w3.org/ns/dcat#Dataset> ;
        <http://purl.org/dc/terms/accessRights>
                <http://publications.europa.eu/resource/authority/access-right/PUBLIC> ;
        <http://purl.org/dc/terms/description>
                "Description of dataset 0"@nb ;
        <http://purl.org/dc/terms/identifier>
                "adb4cf00-31c8-460c-9563-55f204cf8221" ;
        <http://purl.org/dc/terms/issued>
                "2019-03-22T13:11:16.546902"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        <http://purl.org/dc/terms/language>
                <http://publications.europa.eu/resource/authority/language/NOR> ;
        <http://purl.org/dc/terms/temporal>
                <https://testdirektoratet.no/model/periodoftime/1> ;
        <http://purl.org/dc/terms/title>
                "Dataset 0"@nb ;
        <http://www.w3.org/ns/dcat#contactPoint>
                <https://testdirektoratet.no/model/contact/1> ;
        <http://www.w3.org/ns/dcat#distribution>
                [ a       <http://www.w3.org/ns/dcat#Distribution> ;
                  <http://purl.org/dc/terms/format>
                          "CSV JSON JSONP YAML XML" ;
                  <http://purl.org/dc/terms/license>
                          <https://data.norge.no/nlod/no> ;
                  <http://purl.org/dc/terms/title>
                          "Test distribution 1" ;
                  <http://www.w3.org/ns/dcat#accessURL>
                          <http://testdirektoratet.no/data/test/1>
                ] ;
        <http://www.w3.org/ns/dcat#distribution>
                [ a       <http://www.w3.org/ns/dcat#Distribution> ;
                  <http://purl.org/dc/terms/format>
                          "CSV JSON JSONP YAML XML" ;
                  <http://purl.org/dc/terms/license>
                          <https://data.norge.no/nlod/no> ;
                  <http://purl.org/dc/terms/title>
                          "Test distribution 2" ;
                  <http://www.w3.org/ns/dcat#accessURL>
                          <http://testdirektoratet.no/data/test/2>
                ] ;
        <http://www.w3.org/ns/dcat#endpointDescription>
                <https://testdirektoratet.no/openapi/dataset/1.yaml> ;
        <http://www.w3.org/ns/dcat#keyword>
                "test" , "fest" ;
        <http://www.w3.org/ns/dcat#theme>
                <http://publications.europa.eu/resource/authority/data-theme/GOVE> , <http://publications.europa.eu/resource/authority/data-theme/TECH> ;
        <http://xmlns.com/foaf/0.1/page>
                <https://testdirektoratet.no> .

<https://testdirektoratet.no/model/periodoftime/1>
        a       <http://purl.org/dc/terms/PeriodOfTime> ;
        <http://www.w3.org/ns/dcat#startDate>
                "2019-04-02T00:00:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> .

<https://testdirektoratet.no/model/contact/1>
        a       <http://www.w3.org/2006/vcard/ns#Organization> ;
        <http://www.w3.org/2006/vcard/ns#hasOrganizationName>
                "Testdirektoratet"@nb ;
        <http://www.w3.org/2006/vcard/ns#hasURL>
                <https://testdirektoratet.no> .
""".trim()