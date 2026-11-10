package com.restaurant.saas.menu.dto;

import com.restaurant.saas.menu.entity.ItemAttribute;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemImage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MenuItemResponse(
    Long id,
    Long categoryId,
    String sku,
    String name,
    String description,
    BigDecimal price,
    BigDecimal cost,
    String currency,
    String imageUrl,
    Boolean isAvailable,
    Boolean isFeatured,
    Integer prepTimeMinutes,
    Integer calories,
    Integer sortOrder,
    Integer stockQty,
    List<AttributeView> attributes,
    List<ImageView> images,
    Instant updatedAt
) {

    public record AttributeView(String key, String value) {
    }

    public record ImageView(String url, String label, Integer sortOrder) {
    }

    public static MenuItemResponse from(
        MenuItem item, List<ItemAttribute> attributes, List<MenuItemImage> images) {
        return new MenuItemResponse(
            item.getId(),
            item.getCategoryId(),
            item.getSku(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.getCost(),
            item.getCurrency(),
            item.getImageUrl(),
            item.getIsAvailable(),
            item.getIsFeatured(),
            item.getPrepTimeMinutes(),
            item.getCalories(),
            item.getSortOrder(),
            item.getStockQty(),
            attributes.stream()
                .map(attribute -> new AttributeView(attribute.getAttrKey(), attribute.getAttrValue()))
                .toList(),
            images.stream()
                .map(image -> new ImageView(image.getImageUrl(), image.getLabel(), image.getSortOrder()))
                .toList(),
            item.getUpdatedAt()
        );
    }
}
