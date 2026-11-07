package com.restaurant.saas.employee.dto;

import jakarta.validation.constraints.NotNull;

public record TableAssignmentRequest(
    @NotNull Long tableId,
    @NotNull Long employeeId,
    @NotNull Long branchId
) {
}
