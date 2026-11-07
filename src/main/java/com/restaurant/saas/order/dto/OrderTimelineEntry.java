package com.restaurant.saas.order.dto;

import java.time.Instant;

public record OrderTimelineEntry(
    String fromStatus,
    String toStatus,
    Instant changedAt,
    String changedBy
) {
}
