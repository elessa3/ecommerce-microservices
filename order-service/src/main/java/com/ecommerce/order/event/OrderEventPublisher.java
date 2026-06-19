package com.ecommerce.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.order-events}")
    private String orderEventsTopic;

    public void publish(OrderEvent event) {
        String key = event.orderNumber();

        kafkaTemplate.send(orderEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published {} for order {} to partition {}",
                                event.getClass().getSimpleName(),
                                event.orderNumber(),
                                result.getRecordMetadata().partition());
                    } else {
                        log.error("Failed to publish {} for order {}: {}",
                                event.getClass().getSimpleName(), event.orderNumber(), ex.getMessage());
                    }
                });
    }
}