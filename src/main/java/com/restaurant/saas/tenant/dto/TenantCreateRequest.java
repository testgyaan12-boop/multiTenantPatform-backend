package com.restaurant.saas.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantCreateRequest(
    @NotBlank @Size(min = 2, max = 120) String name,
    @NotBlank
    @Size(min = 2, max = 60)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be lowercase letters, numbers and hyphens")
    String slug,
    @NotBlank @Email String ownerEmail,
    @NotBlank @Size(max = 80) String ownerFirstName,
    @NotBlank @Size(max = 80) String ownerLastName,
    @NotBlank String planCode,
    @Size(max = 64) String timezone,
    @Size(min = 3, max = 3) String currency,
    @Pattern(
        regexp = "^(RESTAURANT|ELECTRONICS|VEHICLE|REAL_ESTATE)$",
        message = "must be one of RESTAURANT, ELECTRONICS, VEHICLE, REAL_ESTATE"
    )
    String businessType
) {
}
