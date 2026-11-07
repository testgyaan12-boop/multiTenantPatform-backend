package com.restaurant.saas.restaurant.dto;

import java.math.BigDecimal;

public record PublicRestaurantResponse(
    String slug,
    String name,
    String description,
    String logoUrl,
    String currency,
    BigDecimal taxRate,
    BigDecimal serviceChargeRate
) {
}
