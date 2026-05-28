package com.its.gestioneordinirestclient.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up RabbitMQ messaging infrastructure.
 * <p>
 * This class defines the necessary AMQP components including exchanges, queues,
 * routing bindings, and serialization mechanisms (JSON) needed to enable
 * asynchronous communication for order processing and payment results.
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Declares the direct exchange used to dispatch outgoing order requests
     * (such as payment processing requests) originating from this service.
     *
     * @return a {@link DirectExchange} named "exchange-orders"
     */
    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange("exchange-orders");
    }

    /**
     * Declares a durable queue dedicated to receiving payment result messages
     * sent from the external payment system.
     *
     * @return a durable {@link Queue} named "queue-payment-results"
     */
    @Bean
    public Queue paymentResultsQueue() {
        return new Queue("queue-payment-results", true); // durable = true
    }

    /**
     * Declare the results exchange here too so the topology exists regardless
     * of which service starts first. RabbitMQ is idempotent on declarations.
     *
     * @return a {@link DirectExchange} named "exchange-payment-results"
     */
    @Bean
    public DirectExchange paymentResultsExchange() {
        return new DirectExchange("exchange-payment-results");
    }

    /**
     * Binds the payment results queue to the payment results exchange using a
     * specific routing key. This ensures messages routed with "payment.status.updated"
     * land safely in the results queue.
     *
     * @param paymentResultsQueue the queue to bind
     * @param paymentResultsExchange the direct exchange to bind to
     * @return a {@link Binding} defining the relationship between the queue and exchange
     */
    @Bean
    public Binding paymentResultsBinding(Queue paymentResultsQueue,
                                         DirectExchange paymentResultsExchange) {
        return BindingBuilder.bind(paymentResultsQueue)
                .to(paymentResultsExchange)
                .with("payment.status.updated");
    }

    /**
     * Configures a message converter that utilizes Jackson to automatically
     * serialize and deserialize Java object payloads to and from JSON format.
     *
     * @return a {@link MessageConverter} backed by Jackson
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Configures and prepares the {@link RabbitTemplate} used for publishing messages.
     * Applies the custom JSON message converter to ensure standardized payload formats.
     *
     * @param connectionFactory the underlying AMQP {@link ConnectionFactory}
     * @param jsonMessageConverter the strategy to use for converting messages
     * @return a fully configured {@link RabbitTemplate} instance
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Configures the container factory responsible for managing the lifecycle of
     * asynchronous message listeners (e.g., methods annotated with {@code @RabbitListener}).
     * Sets up the proper connection factory and message converter requirements.
     *
     * @param connectionFactory the underlying AMQP {@link ConnectionFactory}
     * @param jsonMessageConverter the strategy to use for deserializing incoming payloads
     * @return a {@link SimpleRabbitListenerContainerFactory} instance
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}