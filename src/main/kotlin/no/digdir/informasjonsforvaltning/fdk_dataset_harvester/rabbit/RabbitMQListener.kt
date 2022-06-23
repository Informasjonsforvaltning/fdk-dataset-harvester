package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.harvester.HarvesterActivity
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.HarvestAdminParameters
import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.RabbitHarvestTrigger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(RabbitMQListener::class.java)

@Service
class RabbitMQListener(
    private val harvesterActivity: HarvesterActivity
) {

    @RabbitListener(queues = ["#{receiverQueue.name}"])
    fun receiveDatasetHarvestTrigger(body: RabbitHarvestTrigger, message: Message) {
        logger.info("Received message with key ${message.messageProperties.receivedRoutingKey}")

        val params = HarvestAdminParameters(
            dataSourceId = body.dataSourceId,
            publisherId = body.publisherId,
            dataSourceType = body.dataSourceType
        )

        harvesterActivity.initiateHarvest(params, body.forceUpdate)
    }

}
