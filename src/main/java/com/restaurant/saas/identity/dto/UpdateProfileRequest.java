package com.restaurant.saas.identity.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    @Size(max = 30) String phone,
    @Size(max = 500) String avatarUrl,
    @Size(max = 80) String displayName,
    @Size(max = 500) String bio
) {
}
