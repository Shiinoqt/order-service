package com.its.gestioneordinirestclient.repository;

import com.its.gestioneordinirestclient.dto.OrderRequestDTO;
import com.its.gestioneordinirestclient.model.Order;
import com.its.gestioneordinirestclient.model.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<OrderRequestDTO> findByStatus(StatusEnum status);

    List<Order> findByCustomerEmail(String email);
}
