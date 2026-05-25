package com.its.gestioneordinirestclient.service;

import com.its.gestioneordinirestclient.dto.OrderRequestDTO;
import com.its.gestioneordinirestclient.dto.OrderResponseDTO;
import com.its.gestioneordinirestclient.dto.PaymentRequest;
import com.its.gestioneordinirestclient.dto.PaymentResponse;
import com.its.gestioneordinirestclient.exception.NotFoundException;
import com.its.gestioneordinirestclient.exception.PaymentFailedException;
import com.its.gestioneordinirestclient.mapper.OrderMapper;
import com.its.gestioneordinirestclient.model.Order;
import com.its.gestioneordinirestclient.model.StatusEnum;
import com.its.gestioneordinirestclient.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final RestClient paymentRestClient;

    public OrderResponseDTO getById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return orderMapper.toResponseDTO(order);
    }

    public List<OrderResponseDTO> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    public OrderResponseDTO create(OrderRequestDTO dto) {
        Order order = Order.builder()
                .description(dto.getDescription())
                .status(StatusEnum.UNPAID)
                .total(dto.getTotal())
                .build();

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }

    // Inside your Order Service app (OrderService.java)
    public OrderResponseDTO pay(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        if (StatusEnum.PAID.equals(order.getStatus()) || StatusEnum.PROCESSING.equals(order.getStatus())) {
            throw new PaymentFailedException("Order is already paid or currently being processed.");
        }

        // Set a transitional status while waiting for the network call to succeed
        order.setStatus(StatusEnum.PROCESSING);
        orderRepository.save(order);

        PaymentRequest request = new PaymentRequest(order.getId(), order.getTotal());

        try {
            // Call Payment microservice
            paymentRestClient.post()
                    .uri("/payments")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Fallback if the entire payment microservice is unreachable
            order.setStatus(StatusEnum.UNPAID);
            orderRepository.save(order);
            throw new PaymentFailedException("Payment gateway unreachable: " + e.getMessage());
        }

        Order updatedOrder = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
        return orderMapper.toResponseDTO(updatedOrder);
    }

    public OrderResponseDTO update(UUID id, OrderRequestDTO dto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        existingOrder.setDescription(dto.getDescription());
        existingOrder.setTotal(dto.getTotal());

        Order updatedOrder = orderRepository.save(existingOrder);
        return orderMapper.toResponseDTO(updatedOrder);
    }

    public void delete(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(StatusEnum.DELETED);
        orderRepository.save(order);
    }

    public OrderResponseDTO updateStatusFromExternal(UUID id, StatusEnum newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return orderMapper.toResponseDTO(updatedOrder);
    }
}