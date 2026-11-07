package com.restaurant.saas.payment.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Refund extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "provider_refund_id")
    private String providerRefundId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status = "PENDING";

    private String reason;

    @Column(name = "processed_at")
    private Instant processedAt;
}
