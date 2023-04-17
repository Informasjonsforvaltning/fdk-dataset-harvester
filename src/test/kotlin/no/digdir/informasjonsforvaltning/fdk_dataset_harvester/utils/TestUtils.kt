package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils.ApiTestContext.Companion.mongoContainer
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.springframework.http.HttpStatus
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL


fun apiGet(port: Int, endpoint: String, acceptHeader: String?): Map<String,Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        if(acceptHeader != null) connection.setRequestProperty("Accept", acceptHeader)
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

fun authorizedPost(port: Int, endpoint: String, token: String?, headers: Map<String, String>): Map<String,Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if(!token.isNullOrEmpty()) connection.setRequestProperty("Authorization", "Bearer $token")

        connection.requestMethod = "POST"
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

private fun isOK(response: Int?): Boolean = HttpStatus.resolve(response ?: 0)?.is2xxSuccessful ?: false

fun resetDB() {
    val connectionString = ConnectionString("mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:${mongoContainer.getMappedPort(MONGO_PORT)}/datasetHarvester?authSource=admin&authMechanism=SCRAM-SHA-1")
    val pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()))

    val client: MongoClient = MongoClients.create(connectionString)
    val mongoDatabase = client.getDatabase("datasetHarvester").withCodecRegistry(pojoCodecRegistry)

    val miscCollection = mongoDatabase.getCollection("turtle")
    miscCollection.deleteMany(org.bson.Document())
    miscCollection.insertMany(turtleDBPopulation())

    val catalogCollection = mongoDatabase.getCollection("catalogMeta")
    catalogCollection.deleteMany(org.bson.Document())
    catalogCollection.insertMany(catalogDBPopulation())

    val datasetCollection = mongoDatabase.getCollection("datasetMeta")
    datasetCollection.deleteMany(org.bson.Document())
    datasetCollection.insertMany(datasetDBPopulation())

    client.close()
}
