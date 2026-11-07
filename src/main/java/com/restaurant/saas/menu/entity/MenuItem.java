package com.restaurant.saas.menu.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class MenuItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = Boolean.TRUE;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = Boolean.FALSE;

    @Column(name = "is_vegetarian", nullable = false)
    private Boolean isVegetarian = Boolean.FALSE;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    private Integer calories;

    @Column(name = "stock_qty")
    private Integer stockQty;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
