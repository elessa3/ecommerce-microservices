package com.ecommerce.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the order state machine in isolation — no Spring context needed.
 * This is the kind of test that demonstrates you understand WHY the
 * transitionTo() guard exists, not just that it compiles.
 */
class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} should be allowed")
    @MethodSource("validTransitions")
    @DisplayName("Valid transitions should succeed")
    void validTransition_succeeds(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);

        order.transitionTo(to);

        assertThat(order.getStatus()).isEqualTo(to);
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
            Arguments.of(OrderStatus.PENDING,   OrderStatus.CONFIRMED),
            Arguments.of(OrderStatus.PENDING,   OrderStatus.CANCELLED),
            Arguments.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED),
            Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            Arguments.of(OrderStatus.SHIPPED,   OrderStatus.DELIVERED)
        );
    }

    @ParameterizedTest(name = "{0} -> {1} should be rejected")
    @MethodSource("invalidTransitions")
    @DisplayName("Invalid transitions should throw IllegalStateException")
    void invalidTransition_throws(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);

        assertThatThrownBy(() -> order.transitionTo(to))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(from.toString())
            .hasMessageContaining(to.toString());

        // Critically: the status must NOT have changed after a rejected transition
        assertThat(order.getStatus()).isEqualTo(from);
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
            // Cannot cancel once shipped — the package is already on its way
            Arguments.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            // Cannot go backwards
            Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PENDING),
            Arguments.of(OrderStatus.SHIPPED, OrderStatus.PENDING),
            // Terminal states cannot transition anywhere
            Arguments.of(OrderStatus.DELIVERED, OrderStatus.SHIPPED),
            Arguments.of(OrderStatus.CANCELLED, OrderStatus.PENDING),
            // Cannot skip states
            Arguments.of(OrderStatus.PENDING, OrderStatus.SHIPPED),
            Arguments.of(OrderStatus.PENDING, OrderStatus.DELIVERED)
        );
    }

    @Test
    @DisplayName("Terminal states should have no allowed next states")
    void terminalStates_haveNoTransitions() {
        assertThat(OrderStatus.DELIVERED.allowedNextStates()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedNextStates()).isEmpty();
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
            .orderNumber("ORD-2026-00001")
            .customerId(1L)
            .status(status)
            .totalAmount(BigDecimal.TEN)
            .currency("EUR")
            .build();
    }
}
