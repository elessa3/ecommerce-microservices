package com.ecommerce.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IMPORTANT: this is a copy of the event contract published by order-service.
 *
 * In a real production system, this would live in a separate shared library
 * (e.g. "ecommerce-events-contract") published to a private Maven repository,
 * so both services depend on the SAME class instead of two manually-synced copies.
 *
 * For this portfolio project, duplicating it keeps each service's source
 * independently buildable without extra infra — but the comment above is
 * exactly the kind of trade-off worth explaining in an interview.
 */
public sealed interface OrderEvent {

    String orderNumber();
    LocalDateTime occurredAt();

    record OrderPlaced(
        String orderNumber,
        Long customerId,
        BigDecimal totalAmount,
        String currency,
        List<ItemSnapshot> items,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderConfirmed(
        String orderNumber,
        Long customerId,
        String paymentIntentId,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderShipped(
        String orderNumber,
        Long customerId,
        String trackingNumber,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderCancelled(
        String orderNumber,
        Long customerId,
        String reason,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record ItemSnapshot(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
}
