package com.restaurant.saas.employee.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeUpdateRequest(
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    @Size(max = 30) String phone,
    String roleCode,
    Long branchId,
    @Size(max = 80) String jobTitle,
    LocalDate hiredAt,
    @Size(max = 60) String shiftName,
    String status
) {
}
