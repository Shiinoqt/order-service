package com.its.gestioneordinirestclient.controller;

import com.its.gestioneordinirestclient.dto.OrderRequestDTO;
import com.its.gestioneordinirestclient.dto.OrderResponseDTO;
import com.its.gestioneordinirestclient.dto.PaymentResponse;
import com.its.gestioneordinirestclient.model.StatusEnum;
import com.its.gestioneordinirestclient.security.RequiresAdmin;
import com.its.gestioneordinirestclient.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for order operations.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Returns one order visible to the authenticated user.
     *
     * @param id order identifier
     * @param email authenticated user email from gateway header
     * @return requested order
     */
    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable UUID id,
            @RequestHeader(value = "Auth-Email", required = false) String email,
            @RequestHeader(value = "Auth-Roles", required = false) String rolesHeader) {
        return ResponseEntity.ok(orderService.getById(id, email, rolesHeader));
    }

    /**
     * Returns all orders for administrators.
     *
     * @return all orders
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders(
            @RequestHeader(value = "Auth-Email", required = false) String email,
            @RequestHeader(value = "Auth-Roles", required = false) String rolesHeader) {
        return ResponseEntity.ok(orderService.getAll(email, rolesHeader));
    }

    /**
     * Creates a new order for the authenticated user.
     *
     * @param orderRequestDTO order payload
     * @param email authenticated user email from gateway header
     * @return created order
     */
    @PostMapping(path = "/create", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO orderRequestDTO,
            @RequestHeader(value = "Auth-Email", required = false) String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(orderRequestDTO, email));
    }

    /**
     * Starts payment for an order visible to the authenticated user.
     *
     * @param id order identifier
     * @param email authenticated user email from gateway header
     * @return updated order
     */
    @PostMapping(path = "/pay/{id}", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> payOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "Auth-Email", required = false) String email,
            @RequestHeader(value = "Auth-Roles", required = false) String rolesHeader) {
        return ResponseEntity.ok(orderService.pay(id, email, rolesHeader));
    }

    @PostMapping(path = "/payCheck/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponseDTO> payOrderCheck(
            @PathVariable UUID id,
            @RequestHeader(value = "Auth-Email", required = false) String email,
            @RequestHeader(value = "Auth-Roles", required = false) String rolesHeader,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(orderService.payCheck(id, email, rolesHeader, file));
    }

    /**
     * Updates an order.
     *
     * @param id order identifier
     * @param orderRequestDTO updated payload
     * @return updated order
     */
    @RequiresAdmin
    @PutMapping(path = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        return ResponseEntity.ok(orderService.update(id, orderRequestDTO));
    }

    /**
     * Soft-deletes an order.
     *
     * @param id order identifier
     * @return empty response
     */
    @RequiresAdmin
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates an order status from an external process.
     *
     * @param id order identifier
     * @param status new status
     * @return updated order
     */
    @RequiresAdmin
    @PutMapping(path = "/status/{id}")
    public ResponseEntity<OrderResponseDTO> updateOrderStatusExternal(
            @PathVariable UUID id,
            @RequestParam StatusEnum status) {
        return ResponseEntity.ok(orderService.updateStatusFromExternal(id, status));
    }

    /**
     * Returns payments linked to one order.
     *
     * @param id order identifier
     * @return payment list
     */
    @GetMapping(path = "/payments/{id}", produces = "application/json")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getPayments(id));
    }
}