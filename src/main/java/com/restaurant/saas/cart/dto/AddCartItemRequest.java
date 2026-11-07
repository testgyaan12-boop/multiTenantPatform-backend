package com.restaurant.saas.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddCartItemRequest(
    @NotNull Long menuItemId,
    Long variantId,
    @NotNull @Min(1) Integer quantity,
    @Size(max = 500) String notes,
    @Size(max = 20) List<Long> modifierIds
) {
}
