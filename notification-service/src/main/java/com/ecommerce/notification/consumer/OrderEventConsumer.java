package com.ecommerce.notification.consumer;

import com.ecommerce.notification.service.NotificationService;
import com.ecommerce.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "order-events" topic published by order-service.
 *
 * Receives a generic OrderEventEnvelope (see that class for why), converts
 * it to the correct concrete OrderEvent record, then uses Java 21 pattern
 * matching on the sealed interface to dispatch to the right handler —
 * the compiler guarantees every case is covered.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topic.order-events}", groupId = "notification-service")
    public void onOrderEvent(OrderEventEnvelope envelope) {
        OrderEvent event = envelope.toSpecificEvent();
        log.info("Received {} for order {}", event.getClass().getSimpleName(), event.orderNumber());

        switch (event) {
            case OrderEvent.OrderPlaced placed ->
                notificationService.handleOrderPlaced(placed);

            case OrderEvent.OrderCancelled cancelled ->
                notificationService.handleOrderCancelled(cancelled);

            case OrderEvent.OrderConfirmed confirmed ->
                log.info("Order confirmed: {} — no email configured yet for this event type", confirmed.orderNumber());

            case OrderEvent.OrderShipped shipped ->
                log.info("Order shipped: {} — no email configured yet for this event type", shipped.orderNumber());
        }
    }
}
