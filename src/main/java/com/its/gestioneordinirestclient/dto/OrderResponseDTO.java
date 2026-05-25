package com.its.gestioneordinirestclient.dto;

import com.its.gestioneordinirestclient.model.StatusEnum;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {
    private UUID id;
    private String description;
    private LocalDate creation;
    private StatusEnum status;
    private BigDecimal total;
}
