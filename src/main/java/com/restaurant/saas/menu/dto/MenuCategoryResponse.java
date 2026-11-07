package com.restaurant.saas.menu.dto;

import com.restaurant.saas.menu.entity.MenuCategory;

public record MenuCategoryResponse(
    Long id,
    String name,
    String description,
    String imageUrl,
    Integer sortOrder
) {

    public static MenuCategoryResponse from(MenuCategory category) {
        return new MenuCategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getImageUrl(),
            category.getSortOrder()
        );
    }
}
