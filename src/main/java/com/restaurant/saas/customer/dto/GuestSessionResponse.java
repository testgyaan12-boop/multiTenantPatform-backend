package com.restaurant.saas.customer.dto;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.tenant.entity.Tenant;

import java.time.Instant;

public record GuestSessionResponse(
    String sessionToken,
    Instant expiresAt,
    Long tableId,
    String tableNumber,
    Long branchId,
    String branchName,
    Long tenantId,
    String restaurantSlug,
    String restaurantName
) {

    public static GuestSessionResponse of(
        String token, Instant expiresAt, DiningTable table, Branch branch,
        Tenant tenant, Restaurant restaurant) {
        return new GuestSessionResponse(
            token,
            expiresAt,
            table.getId(),
            table.getTableNumber(),
            branch.getId(),
            branch.getName(),
            branch.getTenantId(),
            tenant.getSlug(),
            restaurant == null ? null : restaurant.getName()
        );
    }

    public static GuestSessionResponse pickup(
        String token, Instant expiresAt, Branch branch, Tenant tenant, Restaurant restaurant) {
        return new GuestSessionResponse(
            token,
            expiresAt,
            null,
            null,
            branch.getId(),
            branch.getName(),
            tenant.getId(),
            tenant.getSlug(),
            restaurant.getName()
        );
    }
}
