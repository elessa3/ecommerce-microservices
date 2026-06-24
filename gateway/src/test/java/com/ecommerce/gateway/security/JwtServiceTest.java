package com.ecommerce.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
    }

    @Test
    @DisplayName("Should validate a correctly signed token and extract claims")
    void validToken_extractsClaims() {
        String token = buildToken("user-123", "ADMIN", Instant.now().plusSeconds(3600));

        Claims claims = jwtService.validateAndExtractClaims(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo("user-123");
        assertThat(jwtService.extractRole(claims)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should default to USER role when no role claim is present")
    void noRoleClaim_defaultsToUser() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
            .subject("user-456")
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(key)
            .compact();

        Claims claims = jwtService.validateAndExtractClaims(token);

        assertThat(jwtService.extractRole(claims)).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should reject an expired token")
    void expiredToken_throws() {
        String expiredToken = buildToken("user-123", "USER", Instant.now().minusSeconds(10));

        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(expiredToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should reject a token signed with a different key")
    void wrongSigningKey_throws() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
            "a-completely-different-secret-key-also-long-enough".getBytes(StandardCharsets.UTF_8));
        String tokenSignedWithWrongKey = Jwts.builder()
            .subject("user-123")
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(wrongKey)
            .compact();

        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(tokenSignedWithWrongKey))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should reject a malformed token string")
    void malformedToken_throws() {
        assertThatThrownBy(() -> jwtService.validateAndExtractClaims("not-a-real-jwt"))
            .isInstanceOf(JwtException.class);
    }

    private String buildToken(String userId, String role, Instant expiry) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }
}
