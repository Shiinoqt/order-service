package com.its.gestioneordinirestclient.service;

import com.its.gestioneordinirestclient.config.RabbitMQConfig;
import com.its.gestioneordinirestclient.dto.PaymentResponse;
import com.its.gestioneordinirestclient.model.PaymentStatusEnum;
import com.its.gestioneordinirestclient.model.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment result events and updates order status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

    private final OrderService orderService;

    /**
     * Updates the related order when a payment result is received.
     *
     * @param response payment result payload
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULTS_QUEUE)
    public void handlePaymentResult(PaymentResponse response) {
        log.info("Received payment result: orderId={}, status={}", response.getOrderId(), response.getStatus());

        if (response.getOrderId() == null || response.getStatus() == null) {
            log.error("Invalid payment result payload: {}", response);
            return;
        }

        if (response.getStatus() == PaymentStatusEnum.ACCEPTED) {
            orderService.updateStatusFromExternal(response.getOrderId(), StatusEnum.PAID);
        } else {
            orderService.updateStatusFromExternal(response.getOrderId(), StatusEnum.UNPAID);
        }

        log.info("Order status updated for orderId={}", response.getOrderId());
    }
}