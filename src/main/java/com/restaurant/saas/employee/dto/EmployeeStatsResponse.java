package com.restaurant.saas.employee.dto;

import java.util.Map;

public record EmployeeStatsResponse(
    long total,
    long active,
    long onDuty,
    Map<String, Long> byRole
) {
}
