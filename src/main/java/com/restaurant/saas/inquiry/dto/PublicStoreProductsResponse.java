package com.restaurant.saas.inquiry.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicStoreProductsResponse(
    String slug,
    String storeName,
    String businessType,
    String currency,
    String logoUrl,
    List<CategoryView> categories
) {

    public record CategoryView(Long id, String name, List<ProductView> items) {
    }

    public record ProductView(
        Long id,
        Long categoryId,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String imageUrl,
        boolean available,
        boolean featured,
        Integer stockQty,
        List<AttributeView> attributes,
        List<VariantView> variants
    ) {
    }

    public record AttributeView(String key, String value) {
    }

    public record VariantView(Long id, String name, BigDecimal priceDelta, boolean isDefault) {
    }
}
