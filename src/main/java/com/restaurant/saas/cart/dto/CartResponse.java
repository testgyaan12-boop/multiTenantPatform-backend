package com.restaurant.saas.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    Long id,
    String status,
    String currency,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal serviceCharge,
    BigDecimal tipTotal,
    BigDecimal discountTotal,
    BigDecimal grandTotal,
    List<CartItemResponse> items
) {

    public record CartItemResponse(
        Long id,
        Long menuItemId,
        String name,
        Long variantId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal modifierTotal,
        BigDecimal lineTotal,
        String notes,
        List<ModifierChoice> modifiers
    ) {
    }

    public record ModifierChoice(Long modifierId, String name, BigDecimal priceDelta) {
    }
}
