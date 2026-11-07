package com.restaurant.saas.branch.dto;

import com.restaurant.saas.branch.entity.Branch;

import java.time.Instant;

public record BranchResponse(
    Long id,
    String code,
    String name,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    String phone,
    String email,
    Boolean isDefault,
    String status,
    String timezone,
    String openingHours,
    Instant updatedAt,
    Long managerId,
    String managerName,
    String currency
) {

    public static BranchResponse from(Branch branch, String managerName) {
        return new BranchResponse(
            branch.getId(),
            branch.getCode(),
            branch.getName(),
            branch.getAddressLine1(),
            branch.getAddressLine2(),
            branch.getCity(),
            branch.getState(),
            branch.getPostalCode(),
            branch.getCountry(),
            branch.getPhone(),
            branch.getEmail(),
            branch.getIsDefault(),
            branch.getStatus(),
            branch.getTimezone(),
            branch.getOpeningHours(),
            branch.getUpdatedAt(),
            branch.getManagerId(),
            managerName,
            branch.getCurrency()
        );
    }

    public static BranchResponse from(Branch branch) {
        return from(branch, null);
    }
}
