-- V1__initial_schema.sql

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(20)     NOT NULL UNIQUE,   -- human-readable, e.g. ORD-2026-00001
    customer_id     BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL,           -- PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    total_amount    NUMERIC(10, 2)  NOT NULL CHECK (total_amount >= 0),
    currency        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    payment_intent_id  VARCHAR(255),                    -- Stripe PaymentIntent ID
    shipping_address   TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL,           -- references product-service (different DB!)
    product_name    VARCHAR(255)    NOT NULL,            -- denormalized snapshot at order time
    unit_price      NUMERIC(10, 2)  NOT NULL CHECK (unit_price >= 0),
    quantity        INTEGER         NOT NULL CHECK (quantity > 0)
);

-- Audit trail of every status transition — useful for support and debugging
CREATE TABLE order_status_history (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status     VARCHAR(20),
    to_status       VARCHAR(20)     NOT NULL,
    changed_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    reason          VARCHAR(255)
);

CREATE INDEX idx_orders_customer  ON orders(customer_id);
CREATE INDEX idx_orders_status    ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Sequence-backed order number generator
CREATE SEQUENCE order_number_seq START 1;
