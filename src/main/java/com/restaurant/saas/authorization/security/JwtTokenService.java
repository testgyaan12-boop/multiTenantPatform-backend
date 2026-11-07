package com.restaurant.saas.authorization.security;

import com.restaurant.saas.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenService {

    private final SecretKey key;
    private final SecurityProperties properties;

    public JwtTokenService(SecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(decodeSecret(properties.jwt().secret()));
    }

    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to raw UTF-8 handling.
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        return raw;
    }

    public String createAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.jwt().accessTtlSeconds());
        return Jwts.builder()
            .issuer(properties.jwt().issuer())
            .subject(principal.userId().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("email", principal.email())
            .claim("tenantId", principal.tenantId())
            .claim("roles", List.copyOf(principal.roles()))
            .claim("permissions", List.copyOf(principal.permissions()))
            .signWith(key)
            .compact();
    }

    public long accessTtlSeconds() {
        return properties.jwt().accessTtlSeconds();
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            Long tenantId = claims.get("tenantId") == null
                ? null
                : ((Number) claims.get("tenantId")).longValue();
            Set<String> roles = ((List<String>) claims.getOrDefault("roles", List.of()))
                .stream().collect(Collectors.toUnmodifiableSet());
            Set<String> permissions = ((List<String>) claims.getOrDefault("permissions", List.of()))
                .stream().collect(Collectors.toUnmodifiableSet());

            return new AuthenticatedPrincipal(
                Long.valueOf(claims.getSubject()),
                tenantId,
                claims.get("email", String.class),
                roles,
                permissions
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("invalid_token", "The access token is invalid or expired");
        }
    }
}
