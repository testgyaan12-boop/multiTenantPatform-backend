package com.restaurant.saas.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestaurantRegistrationRequest(
    @NotBlank(message = "Restaurant name is required")
    @Size(max = 120)
    String name,

    @NotBlank(message = "Owner first name is required")
    @Size(max = 80)
    String ownerFirstName,

    @NotBlank(message = "Owner last name is required")
    @Size(max = 80)
    String ownerLastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255)
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    String password,

    @Size(max = 60)
    String timezone,

    @Size(min = 3, max = 3)
    String currency
) {
}
