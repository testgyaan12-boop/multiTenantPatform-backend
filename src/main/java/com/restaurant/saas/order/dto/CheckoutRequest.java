package com.restaurant.saas.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CheckoutRequest(
    @NotBlank @Size(max = 120) String customerName,
    @Size(max = 30) String customerPhone,
    @Email @Size(max = 255) String customerEmail,
    @Size(max = 500) String notes,
    BigDecimal tipAmount
) {
}
