package com.ecommerce.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Product Service.
 *
 * JWT validation is handled by the API Gateway — this service trusts
 * the X-User-Id and X-User-Role headers injected by the gateway after
 * successful token validation. This avoids duplicating JWT logic.
 *
 * In production, network policies ensure only the gateway can reach
 * this service directly (not external clients).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controller methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public read endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                // Actuator health/info endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Swagger (development only — restrict in prod)
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .build();
    }
}
