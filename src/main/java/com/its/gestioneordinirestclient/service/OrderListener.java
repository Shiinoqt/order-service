package com.its.gestioneordinirestclient.service;

import com.its.gestioneordinirestclient.config.RabbitMQConfig;
import com.its.gestioneordinirestclient.dto.PaymentResponse;
import com.its.gestioneordinirestclient.model.PaymentStatusEnum;
import com.its.gestioneordinirestclient.model.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment result events and updates order status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CALLER_HEADER = "caller";
    private static final String METHOD_HEADER = "method";
    private static final String URI_HEADER = "uri";

    private final OrderService orderService;

    /**
     * Updates the related order when a payment result is received.
     *
     * @param response payment result payload
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULTS_QUEUE)
    public void handlePaymentResult(PaymentResponse response, Message message) {
        restoreMdc(message);

        try {
            if (!isValid(response)) {
                log.error("event=payment_result_invalid payload={}", response);
                return;
            }

            StatusEnum newStatus = mapStatus(response.getStatus());

            log.info("event=payment_result_received orderId={} paymentStatus={}",
                    response.getOrderId(), response.getStatus());

            orderService.updateStatusFromExternal(response.getOrderId(), newStatus);

            log.info("event=order_status_updated orderId={} newStatus={}",
                    response.getOrderId(), newStatus);
        } finally {
            MDC.clear();
        }
    }

    private void restoreMdc(Message message) {
        MessageProperties props = message.getMessageProperties();

        putIfPresent("correlationId", header(props, CORRELATION_HEADER));
        putIfPresent("caller", defaultIfBlank(header(props, CALLER_HEADER), "rabbitmq"));
        putIfPresent("method", defaultIfBlank(header(props, METHOD_HEADER), "AMQP"));
        putIfPresent("uri", defaultIfBlank(header(props, URI_HEADER), props.getConsumerQueue()));
    }

    private boolean isValid(PaymentResponse response) {
        return response != null
                && response.getOrderId() != null
                && response.getStatus() != null;
    }

    private StatusEnum mapStatus(PaymentStatusEnum paymentStatus) {
        return paymentStatus == PaymentStatusEnum.ACCEPTED
                ? StatusEnum.PAID
                : StatusEnum.UNPAID;
    }

    private String header(MessageProperties props, String name) {
        Object value = props.getHeaders().get(name);
        return value != null ? value.toString() : null;
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}