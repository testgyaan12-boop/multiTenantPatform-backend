package com.restaurant.saas.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemRequest(
    @NotNull Long categoryId,
    @NotBlank @Size(max = 60) String sku,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    @DecimalMin("0.00") BigDecimal cost,
    @Size(max = 500) String imageUrl,
    Boolean isAvailable,
    Boolean isFeatured,
    Integer prepTimeMinutes,
    Integer calories,
    Integer sortOrder,
    Integer stockQty,
    @Valid @Size(max = 30) List<AttributeInput> attributes
) {

    public record AttributeInput(
        @NotBlank @Size(max = 60) String key,
        @NotBlank @Size(max = 255) String value
    ) {
    }
}
