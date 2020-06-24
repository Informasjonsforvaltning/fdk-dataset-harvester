package no.dcat.harvester.settings;

import lombok.RequiredArgsConstructor;
import no.dcat.harvester.service.RabbitMQListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQSettings {

    @Bean
    public RabbitMQListener receiver() {
        return new RabbitMQListener();
    }

    @Bean
    public Queue queue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue sendQueue() {
        return new Queue("harvester.UpdateSearchTrigger", false);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("harvests", false, false);
    }

    @Bean
    public Binding binding(TopicExchange topicExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(topicExchange).with("dataset.publisher.HarvestTrigger");
    }

    @Bean
    public Binding sendBinding(TopicExchange topicExchange, Queue sendQueue) {
        return BindingBuilder.bind(sendQueue).to(topicExchange).with("harvester.UpdateSearchTrigger");
    }

    @Bean
    public AmqpTemplate jsonRabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
