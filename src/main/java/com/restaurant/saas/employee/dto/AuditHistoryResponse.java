package com.restaurant.saas.employee.dto;

import java.time.Instant;

public record AuditHistoryResponse(
    Long id,
    String actorEmail,
    String action,
    String entityType,
    String entityId,
    Instant createdAt
) {
}
