package com.restaurant.saas.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffTaskStatusRequest(
    @NotBlank String status,
    @Size(max = 300) String note
) {
}
