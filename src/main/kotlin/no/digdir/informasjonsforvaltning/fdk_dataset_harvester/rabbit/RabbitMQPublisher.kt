package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.model.UpdateSearchMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(RabbitMQPublisher::class.java)

@Service
class RabbitMQPublisher(private val template: RabbitTemplate) {
    fun send(dbId: String?) {
        try {
            template.convertAndSend("harvests","datasets.harvester.UpdateSearchTrigger", UpdateSearchMessage(dbId))
            LOGGER.debug("Successfully sent UpdateSearchTrigger for $dbId")
        } catch (e: AmqpException) {
            LOGGER.error("Could not trigger search update", e)
        }
    }

    fun sendUpdateAssessmentsMessage() {
        try {
            template.convertAndSend("updates", "assessments.update", "")
            LOGGER.debug("Successfully sent assessments.update message")
        } catch (e: AmqpException) {
            LOGGER.error("Could not trigger assessments update", e)
        }
    }
}
