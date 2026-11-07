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
@Table(name = "branch_menu_items")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class BranchMenuItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = Boolean.TRUE;

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;
}
