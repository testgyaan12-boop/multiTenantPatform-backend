package com.restaurant.saas.employee.dto;

public record StaffTaskRequest(
    String title,
    String description,
    String taskType,
    String priority,
    Long assignedToEmployeeId,
    Long branchId,
    Long tableId,
    Long orderId,
    java.time.Instant dueAt,
    String instructions
) {
}
