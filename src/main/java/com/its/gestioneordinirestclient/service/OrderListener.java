package com.its.gestioneordinirestclient.service;

import com.its.gestioneordinirestclient.dto.PaymentResponse;
import com.its.gestioneordinirestclient.model.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Message listener component responsible for consuming and processing asynchronous messages
 * from RabbitMQ queues. It acts as the event-driven bridge between external payment systems
 * and the internal order domain application logic.
 */
@Component
@RequiredArgsConstructor
public class OrderListener {

    private final OrderService orderService;

    /**
     * Listens to the {@code queue-payment-results} queue to process asynchronous payment outcomes.
     * <p>
     * If the payment status is {@code "ACCEPTED"}, the associated order status is updated to
     * {@link StatusEnum#PAID}. For any other status outcome, the order reverts or stays as
     * {@link StatusEnum#UNPAID}.
     * </p>
     *
     * @param response the {@link PaymentResponse} payload containing the order ID and payment status details
     */
    @RabbitListener(queues = "queue-payment-results")
    public void handlePaymentResult(PaymentResponse response) {
        if ("ACCEPTED".equals(response.getStatus())) {
            orderService.updateStatusFromExternal(response.getOrderId(), StatusEnum.PAID);
        } else {
            orderService.updateStatusFromExternal(response.getOrderId(), StatusEnum.UNPAID);
        }
    }
}