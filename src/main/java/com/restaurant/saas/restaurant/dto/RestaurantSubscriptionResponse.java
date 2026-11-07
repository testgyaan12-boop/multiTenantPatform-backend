package com.restaurant.saas.restaurant.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RestaurantSubscriptionResponse(
    String planCode,
    String planName,
    String status,
    BigDecimal priceMonthly,
    String currency,
    String billingInterval,
    Instant trialEndsAt,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Boolean cancelAtPeriodEnd,
    Integer branchLimit,
    Integer branchUsage,
    String features
) {
}
