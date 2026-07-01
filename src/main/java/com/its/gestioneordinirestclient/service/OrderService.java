package com.its.gestioneordinirestclient.service;

import com.its.gestioneordinirestclient.dto.*;
import com.its.gestioneordinirestclient.exception.AccessDeniedException;
import com.its.gestioneordinirestclient.exception.BadRequestException;
import com.its.gestioneordinirestclient.exception.NotFoundException;
import com.its.gestioneordinirestclient.exception.PaymentFailedException;
import com.its.gestioneordinirestclient.mapper.OrderMapper;
import com.its.gestioneordinirestclient.model.Order;
import com.its.gestioneordinirestclient.model.PaymentStatusEnum;
import com.its.gestioneordinirestclient.model.StatusEnum;
import com.its.gestioneordinirestclient.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for orders.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final RestClient paymentRestClient;
    private final PaymentRequestPublisher paymentRequestPublisher;

    @Value("${admin.email}")
    private String adminEmail;
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private String requireEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Missing Auth-Email header");
        }
        return email.trim();
    }

    private List<String> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean isAdmin(String rolesHeader) {
        return parseRoles(rolesHeader).contains(ROLE_ADMIN);
    }

    private Order loadAllowedOrder(UUID id, String email, String rolesHeader) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        boolean isAdmin = isAdmin(rolesHeader);
        boolean isOwner = email.equals(order.getCustomerEmail());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You can only access your own orders");
        }

        return order;
    }

    private void ensurePayable(Order order) {
        if (StatusEnum.PAID.equals(order.getStatus()) || StatusEnum.PROCESSING.equals(order.getStatus())) {
            throw new PaymentFailedException("Order is already paid or currently being processed.");
        }
    }

    /**
     * Returns one order if the caller is the owner or an admin.
     *
     * @param id order identifier
     * @param email authenticated user email
     * @return order response
     */
    public OrderResponseDTO getById(UUID id, String email, String rolesHeader) {
        String safeEmail = requireEmail(email);
        Order order = loadAllowedOrder(id, safeEmail, rolesHeader);

        return orderMapper.toResponseDTO(order);
    }

    /**
     * Returns all orders.
     *
     * @return order list
     */
    public List<OrderResponseDTO> getAll(String email, String rolesHeader) {
        String safeEmail = requireEmail(email);

        List<Order> orders = isAdmin(rolesHeader)
                ? orderRepository.findAll()
                : orderRepository.findByCustomerEmail(safeEmail);

        return orders.stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    /**
     * Returns payments for one order.
     *
     * @param id order identifier
     * @return payment list
     */
    public List<PaymentResponse> getPayments(UUID id) {
        try {
            return paymentRestClient.get()
                    .uri("/payments/orders/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PaymentResponse>>() {});
        } catch (Exception e) {
            log.error("Failed to retrieve payments for order {}", id, e);
            throw new PaymentFailedException("Failed to get payments for order " + id);
        }
    }

    /**
     * Creates a new unpaid order for the authenticated user.
     *
     * @param dto order payload
     * @param email authenticated user email
     * @return created order
     */
    public OrderResponseDTO create(OrderRequestDTO dto, String email) {
        String safeEmail = requireEmail(email);

        Order order = Order.builder()
                .description(dto.getDescription())
                .customerEmail(safeEmail)
                .status(StatusEnum.UNPAID)
                .total(dto.getTotal())
                .build();

        return orderMapper.toResponseDTO(orderRepository.save(order));
    }

    /**
     * Sends a payment request for an order.
     *
     * @param id order identifier
     * @param email authenticated user email
     * @return updated order
     */
    public OrderResponseDTO pay(UUID id, String email, String rolesHeader) {
        String safeEmail = requireEmail(email);
        Order order = loadAllowedOrder(id, safeEmail, rolesHeader);
        ensurePayable(order);

        PaymentRequest request = new PaymentRequest(
                order.getId(),
                order.getCustomerEmail(),
                order.getTotal()
        );

        try {
            paymentRequestPublisher.publish(request);
            order.setStatus(StatusEnum.PROCESSING);
            orderRepository.save(order);

            log.info("event=payment_request_enqueued orderId={} newStatus={}",
                    order.getId(), order.getStatus());
        } catch (Exception e) {
            log.error("Failed to enqueue payment request for order {}", id, e);
            throw new PaymentFailedException("Could not enqueue payment request: " + e.getMessage());
        }

        return orderMapper.toResponseDTO(order);
    }

    public OrderResponseDTO payCheck(UUID id, String email, String rolesHeader, MultipartFile file) {
        String safeEmail = requireEmail(email);
        Order order = loadAllowedOrder(id, safeEmail, rolesHeader);
        ensurePayable(order);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Check file is required");
        }

        PaymentRequest request = new PaymentRequest(order.getId(), order.getCustomerEmail(), order.getTotal());

        try {
            HttpHeaders paymentPartHeaders = new HttpHeaders();
            paymentPartHeaders.setContentType(MediaType.APPLICATION_JSON);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            HttpHeaders filePartHeaders = new HttpHeaders();
            filePartHeaders.setContentType(MediaType.APPLICATION_PDF);

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("payment", new HttpEntity<>(request, paymentPartHeaders));
            multipartBody.add("file", new HttpEntity<>(fileResource, filePartHeaders));

            PaymentResponse paymentResponse = paymentRestClient.post()
                    .uri("/payments/process-with-check")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(PaymentResponse.class);

            if (paymentResponse == null) {
                throw new PaymentFailedException("Payment service returned no response");
            }

            order.setStatus(paymentResponse.getStatus() == PaymentStatusEnum.ACCEPTED ? StatusEnum.PAID : StatusEnum.UNPAID);
            orderRepository.save(order);

            log.info("event=payment_with_check_processed orderId={} paymentStatus={}", order.getId(), paymentResponse.getStatus());
            return orderMapper.toResponseDTO(order);
        } catch (IOException e) {
            log.error("Failed to read check file for order {}", id, e);
            throw new PaymentFailedException("Could not read uploaded check file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process payment with check for order {}", id, e);
            throw new PaymentFailedException("Could not process payment with check: " + e.getMessage());
        }
    }

    /**
     * Updates order fields.
     *
     * @param id order identifier
     * @param dto updated payload
     * @return updated order
     */
    public OrderResponseDTO update(UUID id, OrderRequestDTO dto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        existingOrder.setDescription(dto.getDescription());
        existingOrder.setTotal(dto.getTotal());

        return orderMapper.toResponseDTO(orderRepository.save(existingOrder));
    }

    /**
     * Marks an order as deleted.
     *
     * @param id order identifier
     */
    public void delete(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(StatusEnum.DELETED);
        orderRepository.save(order);
    }

    /**
     * Updates order status from an external event.
     *
     * @param id order identifier
     * @param newStatus new order status
     * @return updated order
     */
    @Transactional
    public OrderResponseDTO updateStatusFromExternal(UUID id, StatusEnum newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));

        order.setStatus(newStatus);
        orderRepository.save(order);
        return orderMapper.toResponseDTO(order);
    }
}