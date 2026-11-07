package com.restaurant.saas.common.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean enabled, String adminEmail, String adminPassword) {
}
