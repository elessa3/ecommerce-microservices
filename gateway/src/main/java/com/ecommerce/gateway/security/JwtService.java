package com.ecommerce.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWTs at the gateway — the ONLY place in the whole system
 * where token validation happens. Downstream services (product-service,
 * order-service) trust the X-User-Id / X-User-Role headers this gateway
 * injects after a successful validation; they never see the raw JWT.
 *
 * This centralization means: a single place to rotate the signing key,
 * a single place to fix an auth bug, and no duplicated security logic
 * copy-pasted across four codebases.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates the token's signature and expiry, returning its claims.
     * Throws JwtException (or a subtype) on any validation failure —
     * the caller (JwtAuthenticationFilter) decides how to respond.
     */
    public Claims validateAndExtractClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            throw e;
        }
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        Object role = claims.get("role");
        return role != null ? role.toString() : "USER";
    }
}
