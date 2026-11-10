package com.restaurant.saas.menu.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "menu_item_images")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class MenuItemImage extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "image_label", nullable = false, length = 60)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
