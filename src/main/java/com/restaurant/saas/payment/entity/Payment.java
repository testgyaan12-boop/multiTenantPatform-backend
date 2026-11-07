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
@Table(name = "payments")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Payment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "raw_response", columnDefinition = "longtext")
    private String rawResponse;
}
