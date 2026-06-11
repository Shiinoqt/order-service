package com.its.gestioneordinirestclient.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the order service.
 *
 * <p>This service publishes payment requests and consumes payment result events.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_EXCHANGE = "exchange-orders";
    public static final String PAYMENT_REQUEST_ROUTING_KEY = "order.payment.request";

    public static final String PAYMENT_RESULTS_QUEUE = "queue-payment-results";
    public static final String PAYMENT_RESULTS_EXCHANGE = "exchange-payment-results";
    public static final String PAYMENT_RESULTS_ROUTING_KEY = "payment.status.updated";

    /**
     * Exchange used to publish payment requests from the order service.
     *
     * @return direct exchange for order-driven payment requests
     */
    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE);
    }

    /**
     * Queue that receives payment result events for order updates.
     *
     * @return durable payment results queue
     */
    @Bean
    public Queue paymentResultsQueue() {
        return new Queue(PAYMENT_RESULTS_QUEUE, true);
    }

    /**
     * Exchange used by the payment service to publish payment results.
     *
     * @return direct exchange for payment result events
     */
    @Bean
    public DirectExchange paymentResultsExchange() {
        return new DirectExchange(PAYMENT_RESULTS_EXCHANGE);
    }

    /**
     * Binding that routes payment result events into the order service queue.
     *
     * @return binding between payment results exchange and order results queue
     */
    @Bean
    public Binding paymentResultsBinding() {
        return BindingBuilder.bind(paymentResultsQueue())
                .to(paymentResultsExchange())
                .with(PAYMENT_RESULTS_ROUTING_KEY);
    }

    /**
     * AMQP admin used to declare broker resources at startup.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @return admin bean
     */
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * JSON converter used for object serialization and deserialization.
     *
     * @return Jackson-based message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * RabbitTemplate configured for JSON payload publishing.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @param jsonMessageConverter JSON converter
     * @return configured RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Listener container factory configured with the shared JSON converter.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @param jsonMessageConverter JSON converter
     * @return listener container factory
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