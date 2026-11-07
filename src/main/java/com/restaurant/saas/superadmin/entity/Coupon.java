package com.restaurant.saas.superadmin.entity;

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
@Table(name = "coupons")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Coupon extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String code;

    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType = "PERCENT";

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redeemed_count", nullable = false)
    private Integer redeemedCount = 0;

    @Column(name = "tenant_id")
    private Long tenantId;
}
