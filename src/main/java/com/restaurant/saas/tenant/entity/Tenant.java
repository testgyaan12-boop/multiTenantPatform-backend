package com.restaurant.saas.tenant.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "business_type", nullable = false, length = 30)
    private String businessType = "RESTAURANT";

    public boolean isSuspended() {
        return "SUSPENDED".equals(status) || "CANCELLED".equals(status);
    }
}
