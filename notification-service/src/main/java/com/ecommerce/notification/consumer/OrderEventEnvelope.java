package com.ecommerce.notification.consumer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.order.event.OrderEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic landing spot for any incoming order-events message.
 *
 * Holds every possible field across all OrderEvent subtypes as nullable.
 * OrderEventConsumer inspects which fields are present to decide the
 * concrete event type and converts to the proper sealed-interface record
 * via toSpecificEvent(). This avoids needing Java type headers on the wire.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEventEnvelope {

    public String orderNumber;
    public Long customerId;
    public BigDecimal totalAmount;
    public String currency;
    public List<OrderEvent.ItemSnapshot> items;
    public String paymentIntentId;
    public String trackingNumber;
    public String reason;
    public LocalDateTime occurredAt;

    /**
     * Determines the concrete event type from which fields are populated,
     * since each OrderEvent subtype has a distinctive field combination.
     */
    public OrderEvent toSpecificEvent() {
        if (items != null) {
            return new OrderEvent.OrderPlaced(orderNumber, customerId, totalAmount, currency, items, occurredAt);
        }
        if (paymentIntentId != null) {
            return new OrderEvent.OrderConfirmed(orderNumber, customerId, paymentIntentId, occurredAt);
        }
        if (trackingNumber != null) {
            return new OrderEvent.OrderShipped(orderNumber, customerId, trackingNumber, occurredAt);
        }
        if (reason != null) {
            return new OrderEvent.OrderCancelled(orderNumber, customerId, reason, occurredAt);
        }
        throw new IllegalArgumentException(
            "Cannot determine event type for order " + orderNumber + " — no distinguishing field set");
    }
}
