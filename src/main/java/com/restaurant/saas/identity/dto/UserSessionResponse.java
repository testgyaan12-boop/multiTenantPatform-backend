package com.restaurant.saas.identity.dto;

import java.time.Instant;

public record UserSessionResponse(
    Long id,
    String device,
    String userAgent,
    String ipAddress,
    Instant createdAt,
    Instant expiresAt,
    boolean current
) {
}
