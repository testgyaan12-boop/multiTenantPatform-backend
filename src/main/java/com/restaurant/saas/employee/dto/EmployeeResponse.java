package com.restaurant.saas.employee.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EmployeeResponse(
    Long id,
    Long userId,
    String employeeCode,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    Long branchId,
    String branchName,
    List<String> roles,
    String jobTitle,
    String status,
    String accountStatus,
    String shiftName,
    Boolean onDuty,
    Instant onDutySince,
    LocalDate hiredAt,
    Instant lastLoginAt,
    String inviteUrl
) {
}
