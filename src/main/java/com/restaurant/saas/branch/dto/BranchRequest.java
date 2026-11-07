package com.restaurant.saas.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BranchRequest(
    @NotNull Long restaurantId,
    @NotBlank @Size(max = 40) String code,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 200) String addressLine1,
    @Size(max = 200) String addressLine2,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 100) String state,
    @Size(max = 20) String postalCode,
    @Size(min = 2, max = 2) String country,
    @Size(max = 30) String phone,
    @Size(max = 255) String email,
    Boolean isDefault,
    @Size(max = 20) String status,
    @Size(max = 64) String timezone,
    @Size(max = 4000) String openingHours,
    Long managerId,
    @Size(min = 3, max = 3) String currency
) {
}
