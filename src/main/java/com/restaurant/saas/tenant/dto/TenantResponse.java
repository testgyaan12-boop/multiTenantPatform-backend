package com.restaurant.saas.tenant.dto;

import com.restaurant.saas.tenant.entity.Tenant;

import java.time.Instant;

public record TenantResponse(
    Long id,
    String name,
    String slug,
    String status,
    String contactEmail,
    String contactPhone,
    String timezone,
    String currency,
    Instant trialEndsAt,
    Instant createdAt,
    String businessType
) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getStatus(),
            tenant.getContactEmail(),
            tenant.getContactPhone(),
            tenant.getTimezone(),
            tenant.getCurrency(),
            tenant.getTrialEndsAt(),
            tenant.getCreatedAt(),
            tenant.getBusinessType()
        );
    }
}
