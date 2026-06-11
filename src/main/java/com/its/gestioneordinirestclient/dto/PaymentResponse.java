package com.its.gestioneordinirestclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.its.gestioneordinirestclient.model.PaymentStatusEnum;
import com.its.gestioneordinirestclient.model.StatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID orderId;
    private UUID transactionId;
    private String email;
    private BigDecimal amount;
    private PaymentStatusEnum status;
}
