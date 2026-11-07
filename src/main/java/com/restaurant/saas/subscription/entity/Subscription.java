package com.restaurant.saas.subscription.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "subscriptions")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Subscription extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false)
    private String status = "TRIALING";

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd = Boolean.FALSE;

    @Column(nullable = false)
    private String provider = "sandbox";

    @Column(name = "provider_subscription_id")
    private String providerSubscriptionId;
}
