package com.restaurant.saas.identity.dto;

import java.time.Instant;

public record ActivityItemResponse(
    Long id,
    Instant timestamp,
    String action,
    String module,
    String actor,
    String details,
    String result
) {
}
