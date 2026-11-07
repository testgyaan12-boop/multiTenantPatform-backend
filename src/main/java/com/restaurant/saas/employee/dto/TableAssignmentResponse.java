package com.restaurant.saas.employee.dto;

import java.time.Instant;

public record TableAssignmentResponse(
    Long id,
    Long tableId,
    String tableNumber,
    Long branchId,
    Long employeeId,
    String employeeName,
    String employeeRole,
    Instant assignedAt,
    Instant releasedAt
) {
}
