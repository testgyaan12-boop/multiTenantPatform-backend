package com.restaurant.saas.identity.dto;

public record UserPreferencesResponse(
    String theme,
    String timeZone,
    String dateFormat,
    String language,
    boolean emailNotifications,
    boolean inAppNotifications
) {
}
