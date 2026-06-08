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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        OrderResponseDTO response = orderService.getById(id);
        return ResponseEntity.ok(response);
    }

    @RequiresAdmin
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<OrderResponseDTO> response = orderService.getAll();
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/create", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO response = orderService.create(orderRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/pay/{id}", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> payOrder(@PathVariable UUID id) {
        OrderResponseDTO response = orderService.pay(id);
        return ResponseEntity.ok(response);
    }

    @RequiresAdmin
    @PutMapping(path = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderResponseDTO> updateOrder(@PathVariable UUID id, @Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO response = orderService.update(id, orderRequestDTO);
        return ResponseEntity.ok(response);
    }

    @RequiresAdmin
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @RequiresAdmin
    @PutMapping(path = "/status/{id}")
    public ResponseEntity<OrderResponseDTO> updateOrderStatusExternal(
            @PathVariable UUID id,
            @RequestParam StatusEnum status) {

        OrderResponseDTO response = orderService.updateStatusFromExternal(id, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/payments/{id}", produces = "application/json")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable UUID id) {
        List<PaymentResponse> response = orderService.getPayments(id);
        return ResponseEntity.ok(response);
    }
}