package com.restaurant.saas.employee.dto;

import java.util.List;

public record EmployeePerformanceResponse(
    String from,
    String to,
    long tasksAssigned,
    long tasksCompleted,
    Double completionRate,
    Double avgCompletionMinutes,
    Long ordersHandled,
    Double avgConfirmationMinutes,
    Long tablesServed,
    boolean attendanceTracked,
    List<DailyPoint> history
) {

    public record DailyPoint(String date, long assigned, long completed) {
    }
}
