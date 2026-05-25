package com.its.gestioneordinirestclient.dto;

import com.its.gestioneordinirestclient.model.StatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {
    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Total is required")
    @PositiveOrZero(message = "Total must be zero or positive")
    private BigDecimal total;
}