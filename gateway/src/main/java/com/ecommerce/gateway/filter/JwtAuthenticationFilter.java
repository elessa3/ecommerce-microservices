package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.config.AppProperties;
import com.ecommerce.gateway.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter — runs on EVERY request through the gateway, before it's
 * routed to any downstream service.
 *
 * Flow:
 *   1. If the path matches app.public-paths, skip auth entirely (product
 *      browsing and Stripe webhooks don't require a logged-in user).
 *   2. Otherwise, extract and validate the Authorization: Bearer <token> header.
 *   3. On success, inject X-User-Id and X-User-Role headers into the request
 *      before forwarding — downstream services read these instead of the JWT.
 *   4. On failure, short-circuit with 401 — the request never reaches
 *      product-service or order-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtractClaims(token);
            String userId = jwtService.extractUserId(claims);
            String role = jwtService.extractRole(claims);

            // Build a NEW request with the injected headers — ServerHttpRequest is immutable
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path) {
        // GET on /api/v1/products is public (browsing), but POST/PUT/DELETE on
        // the same path (admin actions) still require auth — handled by the
        // per-route @PreAuthorize in product-service itself, since the gateway
        // only enforces "is there a valid token", not fine-grained permissions.
        return appProperties.getPublicPaths().stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Unauthorized request to {}: {}", exchange.getRequest().getURI().getPath(), reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;   // run before other filters (e.g. rate limiting)
    }
}
