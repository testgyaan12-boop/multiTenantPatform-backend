package com.restaurant.saas.menu.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicItemDetailResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String imageUrl,
    Boolean isAvailable,
    Boolean isFeatured,
    Boolean isVegetarian,
    Long categoryId,
    Integer prepTimeMinutes,
    Integer calories,
    List<VariantView> variants,
    List<ModifierGroupView> modifierGroups
) {

    public record VariantView(Long id, String name, BigDecimal priceDelta, Boolean isDefault) {
    }

    public record ModifierGroupView(
        Long id,
        String name,
        String selectionType,
        Boolean isRequired,
        Integer minSelections,
        Integer maxSelections,
        List<ModifierView> modifiers
    ) {
    }

    public record ModifierView(Long id, String name, BigDecimal priceDelta) {
    }
}
