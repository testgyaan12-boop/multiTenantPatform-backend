package com.restaurant.saas.menu.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "modifier_groups")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class ModifierGroup extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "selection_type", nullable = false)
    private String selectionType = "SINGLE";

    @Column(name = "min_selections", nullable = false)
    private Integer minSelections = 0;

    @Column(name = "max_selections", nullable = false)
    private Integer maxSelections = 1;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = Boolean.FALSE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
