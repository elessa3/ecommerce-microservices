package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order lifecycle management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order — creates a Stripe PaymentIntent and returns the client secret")
    public ResponseEntity<OrderDto.CreateResponse> create(@Valid @RequestBody OrderDto.CreateRequest request) {
        OrderDto.CreateResponse response = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getOrder().getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details and current status")
    public OrderDto.Response getById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only allowed in PENDING or CONFIRMED state)")
    public OrderDto.Response cancel(@PathVariable Long id, @Valid @RequestBody OrderDto.CancelRequest request) {
        return orderService.cancelOrder(id, request.getReason());
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark an order as shipped (admin only)")
    public OrderDto.Response ship(@PathVariable Long id, @RequestParam String trackingNumber) {
        return orderService.shipOrder(id, trackingNumber);
    }
}
