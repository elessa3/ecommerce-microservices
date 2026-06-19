package com.ecommerce.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * Generates human-readable order numbers like "ORD-2026-00001".
 * Uses a PostgreSQL sequence (order_number_seq) so it's safe under concurrent requests —
 * the database guarantees each call gets a unique, monotonically increasing number.
 */
@Service
@RequiredArgsConstructor
public class OrderNumberGenerator {

    @PersistenceContext
    private final EntityManager entityManager;

    public String generate() {
        Number nextVal = (Number) entityManager
            .createNativeQuery("SELECT nextval('order_number_seq')")
            .getSingleResult();

        return "ORD-%d-%05d".formatted(Year.now().getValue(), nextVal.longValue());
    }
}
