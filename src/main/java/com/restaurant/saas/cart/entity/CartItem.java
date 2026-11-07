package com.restaurant.saas.cart.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class CartItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "modifier_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal modifierTotal = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    private String notes;

    @Column(name = "modifier_selections", columnDefinition = "LONGTEXT")
    private String modifierSelections;
}
