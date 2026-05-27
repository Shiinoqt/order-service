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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Service class responsible for managing business logic related to orders.
 * It provides operations for creating, retrieving, updating, and soft-deleting orders,
 * as well as interacting with external payment services via REST and RabbitMQ message queues.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final RestClient paymentRestClient;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id the {@link UUID} of the order to retrieve
     * @return the {@link OrderResponseDTO} containing the order details
     * @throws NotFoundException if no order is found with the given ID
     */
    public OrderResponseDTO getById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return orderMapper.toResponseDTO(order);
    }

    /**
     * Retrieves all existing orders in the system.
     *
     * @return a {@link List} of {@link OrderResponseDTO}s
     */
    public List<OrderResponseDTO> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    /**
     * Fetches the payment history for a specific order from an external REST service.
     *
     * @param id the {@link UUID} of the order whose payments are to be retrieved
     * @return a {@link List} of {@link PaymentResponse}s associated with the order
     * @throws PaymentFailedException if the external REST call fails or encounters an error
     */
    public List<PaymentResponse> getPayments(UUID id) {
        try {
            return paymentRestClient.get()
                    .uri("/payments/orders/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PaymentResponse>>() {});
        } catch (Exception e) {
            throw new PaymentFailedException("Failed to get payments for order " + id);
        }
    }

    /**
     * Creates and saves a new order with an initial status of {@code UNPAID}.
     *
     * @param dto the {@link OrderRequestDTO} containing the details of the order to create
     * @return the {@link OrderResponseDTO} representing the newly created order
     */
    public OrderResponseDTO create(OrderRequestDTO dto) {
        Order order = Order.builder()
                .description(dto.getDescription())
                .status(StatusEnum.UNPAID)
                .total(dto.getTotal())
                .build();

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }

    /**
     * Initiates the payment process for a specific order.
     * Verifies eligibility, dispatches a payment request message to RabbitMQ,
     * and sets the order status to {@code PROCESSING}.
     *
     * @param id the {@link UUID} of the order to pay for
     * @return the updated {@link OrderResponseDTO} reflecting the {@code PROCESSING} status
     * @throws NotFoundException if the order does not exist
     * @throws PaymentFailedException if the order is already processed/paid, or if the RabbitMQ message dispatch fails
     */
    public OrderResponseDTO pay(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        if (StatusEnum.PAID.equals(order.getStatus()) || StatusEnum.PROCESSING.equals(order.getStatus())) {
            throw new PaymentFailedException("Order is already paid or currently being processed.");
        }

        PaymentRequest request = new PaymentRequest(order.getId(), order.getTotal());

        try {
            rabbitTemplate.convertAndSend("exchange-orders", "order.payment.request", request);
            order.setStatus(StatusEnum.PROCESSING);
            orderRepository.save(order);
        } catch (Exception e) {
            throw new PaymentFailedException("Could not enqueue payment request: " + e.getMessage());
        }

        return orderMapper.toResponseDTO(order);
    }

    /**
     * Updates the description and total price of an existing order.
     *
     * @param id the {@link UUID} of the order to update
     * @param dto the {@link OrderRequestDTO} containing the updated information
     * @return the updated {@link OrderResponseDTO}
     * @throws NotFoundException if the order to be updated is not found
     */
    public OrderResponseDTO update(UUID id, OrderRequestDTO dto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        existingOrder.setDescription(dto.getDescription());
        existingOrder.setTotal(dto.getTotal());

        Order updatedOrder = orderRepository.save(existingOrder);
        return orderMapper.toResponseDTO(updatedOrder);
    }

    /**
     * Soft-deletes an order by updating its status to {@code DELETED}.
     *
     * @param id the {@link UUID} of the order to delete
     * @throws NotFoundException if the order does not exist
     */
    public void delete(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(StatusEnum.DELETED);
        orderRepository.save(order);
    }

    /**
     * Updates the status of an order based on feedback received from an external system
     * (e.g., asynchronous payment processors).
     *
     * @param id the {@link UUID} of the order to update
     * @param newStatus the new {@link StatusEnum} to assign to the order
     * @return the updated {@link OrderResponseDTO}
     * @throws NotFoundException if the order does not exist
     */
    public OrderResponseDTO updateStatusFromExternal(UUID id, StatusEnum newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(updatedOrder);
    }
}