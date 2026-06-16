package com.its.gestioneordinirestclient.service;

import com.its.gestioneordinirestclient.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
public class PaymentRequestPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(PaymentRequest request) {
        rabbitTemplate.convertAndSend(
                "exchange-orders",
                "order.payment.request",
                request,
                message -> {
                    MessageProperties props = message.getMessageProperties();
                    setIfPresent(props, "X-Correlation-Id", MDC.get("correlationId"));
                    setIfPresent(props, "caller", MDC.get("caller"));
                    setIfPresent(props, "method", MDC.get("method"));
                    setIfPresent(props, "uri", MDC.get("uri"));
                    setIfPresent(props, "orderId", request.getOrderId().toString());
                    return message;
                }
        );
    }

    private void setIfPresent(MessageProperties props, String key, String value) {
        if (value != null && !value.isBlank()) {
            props.setHeader(key, value);
        }
    }
}
