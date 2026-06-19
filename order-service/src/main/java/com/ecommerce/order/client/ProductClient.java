package com.ecommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Calls product-service to validate stock and fetch current prices.
 *
 * Wrapped with Resilience4j circuit breaker + retry:
 * - If product-service is slow/down, fail fast after the threshold instead
 *   of hanging the order-service thread pool.
 * - This is the classic "synchronous call between microservices" problem,
 *   and the circuit breaker is the standard mitigation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestClient restClient;

    @Value("${app.product-service.url}")
    private String productServiceUrl;

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProduct")
    @Retry(name = "productService")
    public ProductInfo getProduct(Long productId) {
        return restClient.get()
            .uri(productServiceUrl + "/api/v1/products/{id}", productId)
            .retrieve()
            .body(ProductInfo.class);
    }

    // Fallback when product-service is unavailable — fail the order creation
    // with a clear error instead of letting the thread hang indefinitely.
    private ProductInfo fallbackGetProduct(Long productId, Exception ex) {
        log.error("Circuit breaker open for product-service. Product {} unavailable: {}",
            productId, ex.getMessage());
        throw new ProductServiceUnavailableException(productId);
    }

    public record ProductInfo(Long id, String sku, String name, BigDecimal price, Integer stock) {}

    public static class ProductServiceUnavailableException extends RuntimeException {
        public ProductServiceUnavailableException(Long productId) {
            super("Product service unavailable — cannot validate product " + productId);
        }
    }
}
