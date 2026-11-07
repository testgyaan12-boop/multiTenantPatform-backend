package com.restaurant.saas.superadmin.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "feature_flags")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class FeatureFlag extends BaseEntity {

    @Column(name = "flag_key", nullable = false, length = 80)
    private String flagKey;

    private String description;

    @Column(nullable = false)
    private Boolean enabled = Boolean.FALSE;

    @Column(name = "plan_code", length = 40)
    private String planCode;
}
