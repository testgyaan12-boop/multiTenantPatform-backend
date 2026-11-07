package com.restaurant.saas.inquiry.dto;

import java.math.BigDecimal;

public record StoreView(
    String slug,
    String name,
    String description,
    String logoUrl,
    String cuisine,
    String contactEmail,
    String contactPhone,
    String currency,
    String timezone,
    String businessType,
    String socialLinks,
    String operatingHours,
    String publicUrl,
    BookingConfig bookingPayment
) {

    public record BookingConfig(boolean enabled, BigDecimal amount) {
    }
}
