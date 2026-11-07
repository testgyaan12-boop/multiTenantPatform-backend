package com.restaurant.saas.cart.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "carts")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Cart extends BaseEntity {

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

    @Column(nullable = false)
    private String status = "OPEN";

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
}
