package com.restaurant.saas.identity.dto;

import java.time.Instant;
import java.util.Set;

public record AuthUserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    Set<String> roles,
    Set<String> permissions,
    Long tenantId,
    String tenantName,
    String phone,
    String avatarUrl,
    String displayName,
    String bio,
    Boolean emailVerified,
    String status,
    Instant lastLoginAt,
    Instant createdAt
) {
}
