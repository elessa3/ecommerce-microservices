package com.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Binds the "app" prefix from application.yml.
 *
 * Using @ConfigurationProperties instead of @Value here because @Value
 * cannot reliably bind YAML LISTS (like public-paths) — it's designed for
 * single scalar values. @ConfigurationProperties handles lists, nested
 * objects, and is also type-safe and testable in isolation.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private List<String> publicPaths;

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
