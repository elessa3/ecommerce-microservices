package com.ecommerce.order.entity;

import java.util.Set;

/**
 * Order lifecycle state machine.
 *
 *   PENDING ──confirm──▶ CONFIRMED ──ship──▶ SHIPPED ──deliver──▶ DELIVERED
 *      │                    │
 *      └──────cancel────────┘
 *                  ▼
 *              CANCELLED
 *
 * Each enum value knows which transitions are legal FROM it.
 * This prevents invalid transitions like SHIPPED → PENDING at compile-time logic level.
 */
public enum OrderStatus {

    PENDING {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return Set.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return Set.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return Set.of(DELIVERED);   // cannot cancel once shipped
        }
    },
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return Set.of();   // terminal state
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return Set.of();   // terminal state
        }
    };

    public abstract Set<OrderStatus> allowedNextStates();

    public boolean canTransitionTo(OrderStatus target) {
        return allowedNextStates().contains(target);
    }
}
