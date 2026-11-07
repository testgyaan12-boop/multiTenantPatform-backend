package com.restaurant.saas.order.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_modifiers")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class OrderItemModifier extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "modifier_id")
    private Long modifierId;

    @Column(name = "name_snapshot", nullable = false)
    private String nameSnapshot;

    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;
}
