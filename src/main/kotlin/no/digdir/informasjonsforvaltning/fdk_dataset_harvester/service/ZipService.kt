package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.service

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.xml.bind.DatatypeConverter
import kotlin.text.Charsets.UTF_8

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return DatatypeConverter.printBase64Binary(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = DatatypeConverter.parseBase64Binary(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(UTF_8)
        .use { it.readText() }
}