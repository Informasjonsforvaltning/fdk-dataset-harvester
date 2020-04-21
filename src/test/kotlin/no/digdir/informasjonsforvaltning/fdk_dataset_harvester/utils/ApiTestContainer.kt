package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.utils

import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.io.IOException
import java.time.Duration

abstract class ApiTestContainer {
    companion object {

        private val logger = LoggerFactory.getLogger(ApiTestContainer::class.java)
        var fusekiContainer: KGenericContainer
        var TEST_API: KGenericContainer

        init {

            startMockServer()

            Testcontainers.exposeHostPorts(LOCAL_SERVER_PORT)
            val apiNetwork = Network.newNetwork()

            fusekiContainer = KGenericContainer("eu.gcr.io/digdir-fdk-infra/fdk-fuseki-service:latest")
                .withExposedPorts(API_PORT)
                .waitingFor(HttpWaitStrategy()
                    .forPort(API_PORT)
                    .forPath("/fuseki/dataset")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(1)))
                .withNetwork(apiNetwork)
                .withNetworkAliases(FUSEKI_NETWORK_NAME)

            TEST_API = KGenericContainer(System.getProperty("testImageName") ?: "eu.gcr.io/digdir-fdk-infra/fdk-dataset-harvester:latest")
                .withExposedPorts(API_PORT)
                .dependsOn(fusekiContainer)
                .waitingFor(HttpWaitStrategy()
                    .forPort(API_PORT)
                    .forPath("/count")
                    .forResponsePredicate { response -> response?.let { it.toLong() > 0 } ?: false }
                    .withStartupTimeout(Duration.ofMinutes(1)))
                .withNetwork(apiNetwork)
                .withEnv(API_ENV_VALUES)

            fusekiContainer.start()
            TEST_API.start()

            addTestDataToFuseki(DATASET_0, "dataset?graph=$DATASET_ID_0")
            addTestDataToFuseki(DATASET_1, "dataset?graph=$DATASET_ID_1")
            addTestDataToFuseki(CATALOG_0, "dataset-catalog?graph=$CATALOG_ID_0")
            addTestDataToFuseki(CATALOG_1, "dataset-catalog?graph=$CATALOG_ID_1")

            try {
                val result = TEST_API.execInContainer("wget", "-O", "-", "$WIREMOCK_TEST_HOST/ping")
                if (!result.stderr.contains("200")) {
                    logger.debug("Ping to mock server failed")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

}

// Hack needed because testcontainers use of generics confuses Kotlin
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
