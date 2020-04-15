package no.dcat.harvester.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateSearchService {

    private static final Logger logger = LoggerFactory.getLogger(UpdateSearchService.class);
    private final ApplicationContext context;

    public void updateSearch() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        AmqpTemplate rabbitTemplate = (AmqpTemplate)context.getBean("jsonRabbitTemplate");
        payload.put("updatesearch", "concepts");

        try {
            rabbitTemplate.convertAndSend("harvester.UpdateSearchTrigger", payload);
            logger.info("Successfully sent harvest message for publisher {}", payload);
        } catch (AmqpException e) {
            logger.error("Failed to send harvest message for publisher {}", payload, e);
        }
    }

}
