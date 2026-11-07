package com.restaurant.saas.authorization.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, Cors cors) {

    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlDays, String issuer) {
    }

    public record Cors(List<String> allowedOrigins, boolean allowCredentials) {
    }
}
