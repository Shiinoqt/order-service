package com.its.gestioneordinirestclient.mapper;

import com.its.gestioneordinirestclient.dto.OrderRequestDTO;
import com.its.gestioneordinirestclient.dto.OrderResponseDTO;
import com.its.gestioneordinirestclient.model.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponseDTO toResponseDTO(Order order);
    Order toEntity(OrderRequestDTO dto);
}