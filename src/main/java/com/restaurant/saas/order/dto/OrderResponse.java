package com.restaurant.saas.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    String publicToken,
    String status,
    String paymentStatus,
    String source,
    Long tableId,
    String tableNumber,
    String currency,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal serviceCharge,
    BigDecimal tipTotal,
    BigDecimal discountTotal,
    BigDecimal grandTotal,
    String customerName,
    String customerPhone,
    String notes,
    Instant placedAt,
    Instant acceptedAt,
    Instant readyAt,
    Instant completedAt,
    Instant cancelledAt,
    Instant createdAt,
    List<OrderItemResponse> items
) {

    public record OrderItemResponse(
        Long id,
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String status,
        String notes,
        List<ModifierView> modifiers
    ) {
    }

    public record ModifierView(String name, BigDecimal priceDelta) {
    }
}
