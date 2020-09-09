package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rdf.BACKUP_BASE_URI
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory

import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.nio.charset.StandardCharsets

class TestResponseReader {

    private fun resourceAsReader(resourceName: String): Reader {
        return InputStreamReader(javaClass.classLoader.getResourceAsStream(resourceName)!!, StandardCharsets.UTF_8)
    }

    fun readFile(filename: String): String =
        resourceAsReader(filename).readText()

    fun parseFile(filename: String, lang: String): Model {
        val expected = ModelFactory.createDefaultModel()
        expected.read(resourceAsReader(filename), BACKUP_BASE_URI, lang)
        return expected
    }

    fun parseResponse(response: String, lang: String): Model {
        val responseModel = ModelFactory.createDefaultModel()
        responseModel.read(StringReader(response), BACKUP_BASE_URI, lang)
        return responseModel
    }
}
