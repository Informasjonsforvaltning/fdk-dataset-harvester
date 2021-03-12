package no.digdir.informasjonsforvaltning.fdk_dataset_harvester.rabbit

import org.springframework.amqp.core.AnonymousQueue
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.ExchangeBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RabbitMQConfig {

    @Bean
    open fun receiverQueue(): Queue = AnonymousQueue()

    @Bean
    open fun senderQueue(): Queue = AnonymousQueue()

    @Bean
    open fun converter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    open fun topicExchange(): TopicExchange =
        ExchangeBuilder
            .topicExchange("harvests")
            .durable(false)
            .build()

    @Bean
    open fun binding(topicExchange: TopicExchange?, receiverQueue: Queue?): Binding =
        BindingBuilder
            .bind(receiverQueue)
            .to(topicExchange)
            .with("dataset.*.HarvestTrigger")

    @Bean
    open fun sendBinding(topicExchange: TopicExchange?, senderQueue: Queue?): Binding =
        BindingBuilder
            .bind(senderQueue)
            .to(topicExchange)
            .with("dataset.harvester.UpdateSearchTrigger")
}
