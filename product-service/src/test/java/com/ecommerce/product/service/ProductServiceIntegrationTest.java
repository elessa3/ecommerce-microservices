package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test — spins up REAL PostgreSQL and Elasticsearch in Docker.
 * No mocks, no in-memory databases. This is what European companies expect.
 *
 * @Testcontainers manages the container lifecycle automatically.
 */
@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductServiceIntegrationTest {

    // Testcontainers starts real Docker containers before the tests
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("product_db")
        .withUsername("ecommerce")
        .withPassword("ecommerce");

    @Container
    static ElasticsearchContainer elasticsearch =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
            .withEnv("xpack.security.enabled", "false");

    // Tell Spring Boot to use the Testcontainers URLs instead of the real ones
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",        postgres::getJdbcUrl);
        registry.add("spring.datasource.username",   postgres::getUsername);
        registry.add("spring.datasource.password",   postgres::getPassword);
        registry.add("spring.elasticsearch.uris",    elasticsearch::getHttpHostAddress);
    }

    @Autowired ProductService     productService;
    @Autowired ProductRepository  productRepository;

    private static Long createdProductId;

    @Test
    @Order(1)
    @DisplayName("Should create a product and persist it to PostgreSQL")
    void shouldCreateProduct() {
        ProductDto.CreateRequest request = ProductDto.CreateRequest.builder()
            .sku("TEST-001")
            .name("Test Headphones")
            .description("Great sound quality")
            .price(new BigDecimal("99.99"))
            .stock(25)
            .build();

        ProductDto.Response response = productService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSku()).isEqualTo("TEST-001");
        assertThat(response.getPrice()).isEqualByComparingTo("99.99");

        createdProductId = response.getId();
    }

    @Test
    @Order(2)
    @DisplayName("Should find product by ID (first call hits DB, second hits Redis cache)")
    void shouldFindProductById() {
        ProductDto.Response first  = productService.findById(createdProductId);
        ProductDto.Response second = productService.findById(createdProductId);  // from cache

        assertThat(first.getId()).isEqualTo(createdProductId);
        assertThat(second.getId()).isEqualTo(createdProductId);
        assertThat(first.getName()).isEqualTo("Test Headphones");
    }

    @Test
    @Order(3)
    @DisplayName("Should reject duplicate SKU")
    void shouldRejectDuplicateSku() {
        ProductDto.CreateRequest duplicate = ProductDto.CreateRequest.builder()
            .sku("TEST-001")   // same SKU as before
            .name("Another product")
            .price(new BigDecimal("49.99"))
            .stock(10)
            .build();

        assertThatThrownBy(() -> productService.create(duplicate))
            .hasMessageContaining("TEST-001");
    }

    @Test
    @Order(4)
    @DisplayName("Should soft-delete a product (sets active=false, preserves record)")
    void shouldSoftDeleteProduct() {
        productService.delete(createdProductId);

        // The record still exists in the database
        Product deleted = productRepository.findById(createdProductId).orElseThrow();
        assertThat(deleted.getActive()).isFalse();

        // But it's not accessible through the service
        assertThatThrownBy(() -> productService.findById(createdProductId));
    }
}
