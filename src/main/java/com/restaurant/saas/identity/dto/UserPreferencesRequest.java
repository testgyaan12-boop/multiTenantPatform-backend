package com.restaurant.saas.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPreferencesRequest(
    @NotBlank @Pattern(regexp = "SYSTEM|LIGHT|DARK", message = "theme must be SYSTEM, LIGHT or DARK")
    String theme,
    @NotBlank @Size(max = 64) String timeZone,
    @NotBlank @Pattern(regexp = "DD/MM/YYYY|MM/DD/YYYY|YYYY-MM-DD",
        message = "dateFormat must be DD/MM/YYYY, MM/DD/YYYY or YYYY-MM-DD")
    String dateFormat,
    @NotBlank @Size(max = 10) String language,
    Boolean emailNotifications,
    Boolean inAppNotifications
) {
}
