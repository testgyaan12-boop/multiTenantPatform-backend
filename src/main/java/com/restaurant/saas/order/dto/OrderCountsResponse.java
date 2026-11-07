package com.restaurant.saas.order.dto;

import java.util.Map;

public record OrderCountsResponse(
    long total,
    Map<String, Long> byStatus
) {
}
