package com.restaurant.saas.order.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Order extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "guest_session_id")
    private Long guestSessionId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "public_token", nullable = false, length = 36, columnDefinition = "char(36)")
    private String publicToken = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String status = "PLACED";

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING";

    @Column(nullable = false)
    private String source = "QR";

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "service_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Column(name = "tip_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal tipTotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    private String notes;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;
}
