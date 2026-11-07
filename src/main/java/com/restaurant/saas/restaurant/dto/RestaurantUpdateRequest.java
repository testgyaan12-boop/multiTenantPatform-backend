package com.restaurant.saas.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RestaurantUpdateRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 200) String legalName,
    @Size(max = 1000) String description,
    @Size(max = 500) String logoUrl,
    @Size(max = 500) String coverImageUrl,
    @Size(max = 20000) String gallery,
    @Size(max = 500) String brandColors,
    @Size(max = 64) String timezone,
    @Size(min = 3, max = 3) String currency,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal taxRate,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal serviceChargeRate,
    @Size(max = 200) String cuisine,
    @Size(max = 255) String contactEmail,
    @Size(max = 30) String contactPhone,
    @Size(max = 255) String website,
    @Size(max = 255) String addressLine,
    @Size(max = 100) String city,
    @Size(max = 100) String state,
    @Size(max = 100) String country,
    @Size(max = 20) String postalCode,
    @Size(max = 100) String businessRegistrationNumber,
    @Size(max = 100) String taxNumber,
    @Size(max = 10) String defaultLanguage,
    @Size(max = 4000) String socialLinks,
    @Size(max = 4000) String operatingHours
) {
}
