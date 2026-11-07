package com.restaurant.saas.payment.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments")
public record PaymentProperties(String provider, Stripe stripe) {

    public record Stripe(String apiKey, String webhookSecret) {
    }
}
