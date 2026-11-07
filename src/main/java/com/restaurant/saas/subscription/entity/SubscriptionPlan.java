package com.restaurant.saas.subscription.entity;

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
@Table(name = "subscription_plans")
@SQLRestriction("is_delete = 0")
@Getter
@Setter
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "business_type", nullable = false, length = 30)
    private String businessType = "RESTAURANT";

    @Column(name = "price_monthly", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMonthly = BigDecimal.ZERO;

    @Column(name = "price_quarterly", precision = 10, scale = 2)
    private BigDecimal priceQuarterly;

    @Column(name = "price_yearly", precision = 10, scale = 2)
    private BigDecimal priceYearly;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "trial_days", nullable = false)
    private Integer trialDays = 0;

    @Column(name = "setup_fee", precision = 10, scale = 2)
    private BigDecimal setupFee;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "yearly_discount_percent", precision = 5, scale = 2)
    private BigDecimal yearlyDiscountPercent;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "max_branches")
    private Integer maxBranches = 1;

    @Column(name = "max_employees")
    private Integer maxEmployees = 10;

    @Column(name = "max_menu_items")
    private Integer maxMenuItems = 100;

    @Column(name = "max_orders_per_month")
    private Integer maxOrdersPerMonth;

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb;

    @Column(name = "max_admin_users")
    private Integer maxAdminUsers;

    @Column(name = "max_customers")
    private Integer maxCustomers;

    @Column(columnDefinition = "longtext")
    private String features;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = Boolean.TRUE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
