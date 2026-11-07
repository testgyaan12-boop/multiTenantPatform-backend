package com.restaurant.saas.menu.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "item_attributes")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class ItemAttribute extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "attr_key", nullable = false, length = 60)
    private String attrKey;

    @Column(name = "attr_value", nullable = false, length = 255)
    private String attrValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
