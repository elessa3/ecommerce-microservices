package com.ecommerce.notification.consumer;

import com.ecommerce.order.event.OrderEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderEventEnvelopeTest {

    @Test
    @DisplayName("Envelope with 'items' field resolves to OrderPlaced")
    void resolvesToOrderPlaced() {
        OrderEventEnvelope envelope = new OrderEventEnvelope();
        envelope.orderNumber = "ORD-2026-00001";
        envelope.customerId = 1L;
        envelope.totalAmount = BigDecimal.TEN;
        envelope.currency = "EUR";
        envelope.items = List.of(new OrderEvent.ItemSnapshot(1L, "Test Product", 1, BigDecimal.TEN));
        envelope.occurredAt = LocalDateTime.now();

        OrderEvent result = envelope.toSpecificEvent();

        assertThat(result).isInstanceOf(OrderEvent.OrderPlaced.class);
        assertThat(result.orderNumber()).isEqualTo("ORD-2026-00001");
    }

    @Test
    @DisplayName("Envelope with 'reason' field resolves to OrderCancelled")
    void resolvesToOrderCancelled() {
        OrderEventEnvelope envelope = new OrderEventEnvelope();
        envelope.orderNumber = "ORD-2026-00002";
        envelope.customerId = 1L;
        envelope.reason = "Customer changed their mind";
        envelope.occurredAt = LocalDateTime.now();

        OrderEvent result = envelope.toSpecificEvent();

        assertThat(result).isInstanceOf(OrderEvent.OrderCancelled.class);
        assertThat(((OrderEvent.OrderCancelled) result).reason())
            .isEqualTo("Customer changed their mind");
    }

    @Test
    @DisplayName("Envelope with 'trackingNumber' resolves to OrderShipped")
    void resolvesToOrderShipped() {
        OrderEventEnvelope envelope = new OrderEventEnvelope();
        envelope.orderNumber = "ORD-2026-00003";
        envelope.customerId = 1L;
        envelope.trackingNumber = "TRACK123";
        envelope.occurredAt = LocalDateTime.now();

        OrderEvent result = envelope.toSpecificEvent();

        assertThat(result).isInstanceOf(OrderEvent.OrderShipped.class);
    }

    @Test
    @DisplayName("Envelope with no distinguishing fields throws")
    void noDistinguishingFields_throws() {
        OrderEventEnvelope envelope = new OrderEventEnvelope();
        envelope.orderNumber = "ORD-2026-00004";

        assertThatThrownBy(envelope::toSpecificEvent)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ORD-2026-00004");
    }
}
