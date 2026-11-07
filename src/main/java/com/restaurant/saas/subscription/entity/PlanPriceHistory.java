package com.restaurant.saas.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "plan_price_history")
@Getter
@Setter
public class PlanPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "old_price_monthly", precision = 10, scale = 2)
    private BigDecimal oldPriceMonthly;

    @Column(name = "new_price_monthly", precision = 10, scale = 2)
    private BigDecimal newPriceMonthly;

    @Column(name = "old_price_quarterly", precision = 10, scale = 2)
    private BigDecimal oldPriceQuarterly;

    @Column(name = "new_price_quarterly", precision = 10, scale = 2)
    private BigDecimal newPriceQuarterly;

    @Column(name = "old_price_yearly", precision = 10, scale = 2)
    private BigDecimal oldPriceYearly;

    @Column(name = "new_price_yearly", precision = 10, scale = 2)
    private BigDecimal newPriceYearly;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
