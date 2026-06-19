package com.ecommerce.notification.consumer;

import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Full integration test: a real Kafka broker (Testcontainers) delivers a
 * message, the real @KafkaListener consumes it, and we verify the
 * notification was logged to a real PostgreSQL database.
 *
 * JavaMailSender is mocked — we don't want tests to depend on (or spam)
 * a real SMTP server, but everything else in the pipeline is real.
 */
@SpringBootTest
@Testcontainers
class OrderEventConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("notification_db")
        .withUsername("ecommerce")
        .withPassword("ecommerce");

    @Container
    static ConfluentKafkaContainer kafka =
        new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @MockBean
    JavaMailSender javaMailSender;   // don't send real emails in tests

    @Autowired
    NotificationLogRepository notificationLogRepository;

    @Autowired
    KafkaTemplate<String, Object> testProducerTemplate;

    @Test
    @DisplayName("Publishing an OrderPlaced-shaped message should result in a logged SENT notification")
    void orderPlacedEvent_logsNotification() {
        // Simulate exactly what order-service's producer sends on the wire —
        // a plain map, since the real producer doesn't add Java type headers.
        Map<String, Object> payload = Map.of(
            "orderNumber", "ORD-2026-INT-TEST",
            "customerId", 99,
            "totalAmount", new BigDecimal("59.98"),
            "currency", "EUR",
            "items", List.of(Map.of(
                "productId", 1, "productName", "Test Product",
                "quantity", 2, "unitPrice", new BigDecimal("29.99")
            )),
            "occurredAt", LocalDateTime.now().toString()
        );

        testProducerTemplate.send("order-events", "ORD-2026-INT-TEST", payload);

        // The consumer runs asynchronously — poll until the side effect appears
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean exists = notificationLogRepository.existsByOrderNumberAndNotificationType(
                "ORD-2026-INT-TEST", NotificationLog.NotificationType.ORDER_PLACED
            );
            assertThat(exists).isTrue();
        });

        // Verify the email was actually attempted (mocked sender was called)
        verify(javaMailSender, timeout(5000)).send(any(jakarta.mail.internet.MimeMessage.class));
    }
}
