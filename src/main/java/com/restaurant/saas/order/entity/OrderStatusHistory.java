package com.restaurant.saas.order.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "order_status_history")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class OrderStatusHistory extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_by")
    private String changedBy;

    private String notes;
}
