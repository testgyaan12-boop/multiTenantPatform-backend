package com.restaurant.saas.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuCategoryRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    @Size(max = 500) String imageUrl,
    Integer sortOrder
) {
}
