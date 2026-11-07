package com.restaurant.saas.employee.dto;

import java.time.Instant;

public record StaffTaskResponse(
    Long id,
    Long branchId,
    String branchName,
    String title,
    String description,
    String taskType,
    String priority,
    String status,
    Long assignedToEmployeeId,
    String assignedToName,
    String assignedToRole,
    Long tableId,
    String tableNumber,
    Long orderId,
    String orderNumber,
    Instant dueAt,
    Instant acceptedAt,
    Instant startedAt,
    Instant completedAt,
    Instant cancelledAt,
    String instructions,
    String statusNote,
    Instant createdAt,
    Instant updatedAt,
    Boolean overdue
) {
}
