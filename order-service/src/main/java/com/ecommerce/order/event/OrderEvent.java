package com.ecommerce.order.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Events published to the "order-events" Kafka topic.
 * notification-service (and potentially other future services) consume these.
 *
 * Using a sealed interface lets consumers pattern-match on event type safely.
 */
public sealed interface OrderEvent {

    String orderNumber();
    LocalDateTime occurredAt();

    @Builder
    record OrderPlaced(
        String orderNumber,
        Long customerId,
        BigDecimal totalAmount,
        String currency,
        List<ItemSnapshot> items,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    @Builder
    record OrderConfirmed(
        String orderNumber,
        Long customerId,
        String paymentIntentId,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    @Builder
    record OrderShipped(
        String orderNumber,
        Long customerId,
        String trackingNumber,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    @Builder
    record OrderCancelled(
        String orderNumber,
        Long customerId,
        String reason,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    @Builder
    record ItemSnapshot(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
}
