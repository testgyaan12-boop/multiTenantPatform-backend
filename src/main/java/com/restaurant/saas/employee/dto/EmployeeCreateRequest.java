package com.restaurant.saas.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeCreateRequest(
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 30) String phone,
    @NotBlank String roleCode,
    Long branchId,
    @Size(max = 40) String employeeCode,
    @Size(max = 80) String jobTitle,
    LocalDate hiredAt,
    @Size(max = 60) String shiftName
) {
}
