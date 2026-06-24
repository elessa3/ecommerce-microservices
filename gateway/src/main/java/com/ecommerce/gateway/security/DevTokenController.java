package com.ecommerce.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * DEV-ONLY endpoint to generate test JWTs.
 *
 * This project doesn't include a full user-service with password hashing,
 * registration, etc. — that's a reasonable v2 addition, noted in the README.
 * For now, this lets you generate a valid token to test the gateway's
 * JWT validation and the X-User-Id / X-User-Role header injection end-to-end.
 *
 * Returns Mono<Map<...>> instead of a plain ResponseEntity because the gateway
 * runs on Spring WebFlux (reactive), not Spring MVC — this controller's return
 * type has to fit the reactive stack like everything else in this module.
 *
 * MUST be removed or disabled behind a profile before any real deployment.
 */
@RestController
public class DevTokenController {

    @Value("${app.jwt.secret}")
    private String secret;

    @PostMapping("/dev/token")
    public Mono<Map<String, String>> issueToken(
        @RequestParam String userId,
        @RequestParam(defaultValue = "USER") String role
    ) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plusSeconds(3600)))   // 1 hour
            .signWith(key)
            .compact();

        return Mono.just(Map.of("token", token));
    }
}
