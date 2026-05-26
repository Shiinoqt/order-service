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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Service class responsible for managing the business logic of orders.
 * It handles CRUD operations, order status updates, and interacts with an external
 * payment microservice via {@link RestClient}.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    /**
     * Repository used for database operations on {@link Order} entities.
     */
    private final OrderRepository orderRepository;

    /**
     * Mapper component used to convert between Order entities and Data Transfer Objects (DTOs).
     */
    private final OrderMapper orderMapper;

    /**
     * REST client configured to communicate with the external payment microservice.
     */
    private final RestClient paymentRestClient;

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id the {@link UUID} of the order to retrieve
     * @return the {@link OrderResponseDTO} representing the found order
     * @throws NotFoundException if no order is found with the specified id
     */
    public OrderResponseDTO getById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return orderMapper.toResponseDTO(order);
    }

    /**
     * Retrieves all existing orders.
     *
     * @return a {@link List} of {@link OrderResponseDTO} containing all orders
     */
    public List<OrderResponseDTO> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    /**
     * Fetches the payment history/details associated with a specific order from the payment microservice.
     *
     * @param id the {@link UUID} of the order
     * @return a {@link List} of {@link PaymentResponse} details
     * @throws PaymentFailedException if the external call fails or an error occurs during processing
     */
    public List<PaymentResponse> getPayments(UUID id) {
        try {
            return paymentRestClient.get()
                    .uri("/payments/orders/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PaymentResponse>>() {
                    });
        } catch (Exception e) {
            throw new PaymentFailedException("Failed to get payments for order " + id);
        }
    }

    /**
     * Creates and saves a new order. Newly created orders default to a status of {@link StatusEnum#UNPAID}.
     *
     * @param dto the {@link OrderRequestDTO} containing the details of the order to create
     * @return the {@link OrderResponseDTO} of the newly created and saved order
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
     * Initiates the payment workflow for a specific order.
     * <p>
     * The method checks if the order is eligible for payment, temporarily moves its status to
     * {@link StatusEnum#PROCESSING}, and forwards the payment request to the payment microservice.
     * If the external gateway is unreachable, the order status is safely rolled back to {@link StatusEnum#UNPAID}.
     * </p>
     *
     * @param id the {@link UUID} of the order to pay
     * @return the updated {@link OrderResponseDTO}
     * @throws NotFoundException if the order is not found
     * @throws PaymentFailedException if the order is already processed/paid, or if the external payment gateway is unreachable
     */
    public OrderResponseDTO pay(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        if (StatusEnum.PAID.equals(order.getStatus()) || StatusEnum.PROCESSING.equals(order.getStatus())) {
            throw new PaymentFailedException("Order is already paid or currently being processed.");
        }

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

    /**
     * Updates the description and total price of an existing order.
     *
     * @param id  the {@link UUID} of the order to update
     * @param dto the {@link OrderRequestDTO} containing the updated information
     * @return the {@link OrderResponseDTO} of the updated order
     * @throws NotFoundException if the order to update is not found
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
     * Performs a logical (soft) delete on an order by updating its status to {@link StatusEnum#DELETED}.
     *
     * @param id the {@link UUID} of the order to soft-delete
     * @throws NotFoundException if the order is not found
     */
    public void delete(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(StatusEnum.DELETED);
        orderRepository.save(order);
    }

    /**
     * Updates an order's status based on an external signal or webhook (e.g., async payment updates).
     *
     * @param id        the {@link UUID} of the order to update
     * @param newStatus the new {@link StatusEnum} to apply to the order
     * @return the updated {@link OrderResponseDTO}
     * @throws NotFoundException if the order is not found
     */
    public OrderResponseDTO updateStatusFromExternal(UUID id, StatusEnum newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return orderMapper.toResponseDTO(updatedOrder);
    }
}