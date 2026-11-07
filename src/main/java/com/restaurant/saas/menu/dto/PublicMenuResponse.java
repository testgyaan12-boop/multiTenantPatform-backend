package com.restaurant.saas.menu.dto;

import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.util.List;

public record PublicMenuResponse(
    String restaurantName,
    String restaurantSlug,
    String currency,
    List<CategoryGroup> categories
) {

    public record CategoryGroup(Long id, String name, String description, List<ItemView> items) {
    }

    public record ItemView(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean isAvailable,
        Boolean isFeatured,
        Boolean isVegetarian
    ) {

        public static ItemView from(MenuItem item) {
            return new ItemView(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.getIsAvailable(),
                item.getIsFeatured(),
                item.getIsVegetarian()
            );
        }
    }
}
