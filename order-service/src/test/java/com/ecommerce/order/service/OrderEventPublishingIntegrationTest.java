package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test: real PostgreSQL + real Kafka, both via Testcontainers.
 * Verifies that cancelling an order both updates the database AND publishes
 * the OrderCancelled event to Kafka — the two things must happen atomically
 * from the caller's perspective.
 */
@SpringBootTest
@Testcontainers
class OrderEventPublishingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("order_db")
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
        // Use a fake Stripe key for tests — PaymentService calls are not exercised here
        registry.add("app.stripe.secret-key", () -> "sk_test_fake");
    }

    @Autowired OrderRepository orderRepository;
    @Autowired OrderService    orderService;

    private Consumer<String, OrderEvent> testConsumer;

    @BeforeEach
    void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.order.event");

        testConsumer = new KafkaConsumer<>(props);
        testConsumer.subscribe(java.util.List.of("order-events"));
    }

    @AfterEach
    void tearDownConsumer() {
        testConsumer.close();
    }

    @Test
    @DisplayName("Cancelling an order should update PostgreSQL AND publish OrderCancelled to Kafka")
    void cancelOrder_updatesDatabaseAndPublishesEvent() {
        // Arrange: create an order directly via the repository (skip the Stripe step)
        Order order = Order.builder()
            .orderNumber("ORD-2026-TEST1")
            .customerId(42L)
            .status(OrderStatus.PENDING)
            .totalAmount(java.math.BigDecimal.valueOf(99.99))
            .currency("EUR")
            .build();
        Order saved = orderRepository.save(order);

        // Act
        orderService.cancelOrder(saved.getId(), "Customer changed their mind");

        // Assert — database state
        Order updated = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Assert — Kafka event was actually published and is consumable
        ConsumerRecord<String, OrderEvent> record =
            KafkaTestUtils.getSingleRecord(testConsumer, "order-events", Duration.ofSeconds(10));

        assertThat(record.key()).isEqualTo("ORD-2026-TEST1");
        assertThat(record.value()).isInstanceOf(OrderEvent.OrderCancelled.class);
    }
}
