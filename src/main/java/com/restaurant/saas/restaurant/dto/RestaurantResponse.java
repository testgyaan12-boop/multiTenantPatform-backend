package com.restaurant.saas.restaurant.dto;

import com.restaurant.saas.restaurant.entity.Restaurant;

import java.math.BigDecimal;

public record RestaurantResponse(
    Long id,
    String code,
    String name,
    String legalName,
    String description,
    String logoUrl,
    String coverImageUrl,
    String gallery,
    String brandColors,
    String timezone,
    String currency,
    BigDecimal taxRate,
    BigDecimal serviceChargeRate,
    String cuisine,
    String contactEmail,
    String contactPhone,
    String website,
    String addressLine,
    String city,
    String state,
    String country,
    String postalCode,
    String businessRegistrationNumber,
    String taxNumber,
    String defaultLanguage,
    String socialLinks,
    String operatingHours,
    String publicUrl,
    String businessType,
    String tenantStatus
) {

    public static RestaurantResponse from(Restaurant restaurant, String publicUrl, String businessType,
                                          String tenantStatus) {
        return new RestaurantResponse(
            restaurant.getId(),
            restaurant.getCode(),
            restaurant.getName(),
            restaurant.getLegalName(),
            restaurant.getDescription(),
            restaurant.getLogoUrl(),
            restaurant.getCoverImageUrl(),
            restaurant.getGallery(),
            restaurant.getBrandColors(),
            restaurant.getTimezone(),
            restaurant.getCurrency(),
            restaurant.getTaxRate(),
            restaurant.getServiceChargeRate(),
            restaurant.getCuisine(),
            restaurant.getContactEmail(),
            restaurant.getContactPhone(),
            restaurant.getWebsite(),
            restaurant.getAddressLine(),
            restaurant.getCity(),
            restaurant.getState(),
            restaurant.getCountry(),
            restaurant.getPostalCode(),
            restaurant.getBusinessRegistrationNumber(),
            restaurant.getTaxNumber(),
            restaurant.getDefaultLanguage(),
            restaurant.getSocialLinks(),
            restaurant.getOperatingHours(),
            publicUrl,
            businessType,
            tenantStatus
        );
    }
}
