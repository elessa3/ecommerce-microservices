-- V1__initial_schema.sql

-- Records every notification sent — useful for support ("did the customer
-- receive the confirmation email?") and for preventing duplicate sends.
CREATE TABLE notification_log (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(20)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    notification_type VARCHAR(30)   NOT NULL,   -- ORDER_PLACED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_CANCELLED
    recipient_email VARCHAR(255),
    status          VARCHAR(20)     NOT NULL DEFAULT 'SENT',  -- SENT, FAILED
    error_message   TEXT,
    sent_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_order  ON notification_log(order_number);
CREATE INDEX idx_notification_type  ON notification_log(notification_type);

-- Prevents processing the SAME Kafka event twice if a consumer restarts
-- and reprocesses messages (Kafka delivers "at least once", not "exactly once")
CREATE UNIQUE INDEX idx_notification_dedup
    ON notification_log(order_number, notification_type);
