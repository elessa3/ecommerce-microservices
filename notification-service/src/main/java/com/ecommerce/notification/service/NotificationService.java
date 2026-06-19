package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import com.ecommerce.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Orchestrates notification sending for each order event type.
 *
 * Key design point: deduplication. Kafka guarantees "at least once" delivery —
 * if the consumer crashes after sending an email but before committing the
 * offset, the SAME message will be redelivered on restart. Without a check,
 * the customer would get a duplicate email. The unique index on
 * (order_number, notification_type) in the database is the actual guard;
 * the existsBy check here just avoids the unnecessary email send + email
 * provider cost in the common case.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService                 emailService;
    private final NotificationLogRepository     notificationLogRepository;

    // In a real system this would come from a customer-service lookup by customerId.
    // For this portfolio project we use a fixed demo address — see README for how
    // to wire this up to a real customer service call.
    private static final String DEMO_CUSTOMER_EMAIL = "customer@example.com";

    @Transactional
    public void handleOrderPlaced(OrderEvent.OrderPlaced event) {
        var type = NotificationLog.NotificationType.ORDER_PLACED;

        if (alreadySent(event.orderNumber(), type)) {
            log.info("Skipping duplicate notification for order {} (type={})", event.orderNumber(), type);
            return;
        }

        try {
            emailService.sendHtmlEmail(
                DEMO_CUSTOMER_EMAIL,
                "Order Confirmation - " + event.orderNumber(),
                "order-placed",
                Map.of(
                    "orderNumber", event.orderNumber(),
                    "items", event.items(),
                    "totalAmount", event.totalAmount(),
                    "currency", event.currency()
                )
            );
            logNotification(event.orderNumber(), event.customerId(), type,
                DEMO_CUSTOMER_EMAIL, NotificationLog.NotificationStatus.SENT, null);

        } catch (EmailService.EmailSendException e) {
            log.error("Failed to send order-placed email for {}: {}", event.orderNumber(), e.getMessage());
            logNotification(event.orderNumber(), event.customerId(), type,
                DEMO_CUSTOMER_EMAIL, NotificationLog.NotificationStatus.FAILED, e.getMessage());
            // Don't rethrow — a failed email shouldn't cause endless Kafka redelivery retries.
            // In production: push to a dead-letter topic for manual/automatic retry instead.
        }
    }

    @Transactional
    public void handleOrderCancelled(OrderEvent.OrderCancelled event) {
        var type = NotificationLog.NotificationType.ORDER_CANCELLED;

        if (alreadySent(event.orderNumber(), type)) {
            log.info("Skipping duplicate notification for order {} (type={})", event.orderNumber(), type);
            return;
        }

        try {
            emailService.sendHtmlEmail(
                DEMO_CUSTOMER_EMAIL,
                "Order Cancelled - " + event.orderNumber(),
                "order-cancelled",
                Map.of("orderNumber", event.orderNumber(), "reason", event.reason())
            );
            logNotification(event.orderNumber(), event.customerId(), type,
                DEMO_CUSTOMER_EMAIL, NotificationLog.NotificationStatus.SENT, null);

        } catch (EmailService.EmailSendException e) {
            log.error("Failed to send order-cancelled email for {}: {}", event.orderNumber(), e.getMessage());
            logNotification(event.orderNumber(), event.customerId(), type,
                DEMO_CUSTOMER_EMAIL, NotificationLog.NotificationStatus.FAILED, e.getMessage());
        }
    }

    // OrderConfirmed and OrderShipped follow the same pattern — omitted here for
    // brevity but would be straightforward to add following handleOrderPlaced as a template.

    private boolean alreadySent(String orderNumber, NotificationLog.NotificationType type) {
        return notificationLogRepository.existsByOrderNumberAndNotificationType(orderNumber, type);
    }

    private void logNotification(
        String orderNumber, Long customerId, NotificationLog.NotificationType type,
        String email, NotificationLog.NotificationStatus status, String errorMessage
    ) {
        notificationLogRepository.save(NotificationLog.builder()
            .orderNumber(orderNumber)
            .customerId(customerId)
            .notificationType(type)
            .recipientEmail(email)
            .status(status)
            .errorMessage(errorMessage)
            .build());
    }
}
