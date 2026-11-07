package com.restaurant.saas.employee.dto;

import jakarta.validation.constraints.NotNull;

public record DutyRequest(@NotNull Boolean onDuty) {
}
