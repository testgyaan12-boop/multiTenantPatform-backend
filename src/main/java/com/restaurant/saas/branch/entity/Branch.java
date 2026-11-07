package com.restaurant.saas.branch.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "branches")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Branch extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(nullable = false, length = 2, columnDefinition = "char(2)")
    private String country = "US";

    private String phone;

    private String email;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(name = "opening_hours", columnDefinition = "longtext")
    private String openingHours;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = Boolean.FALSE;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "manager_id")
    private Long managerId;

    @Column(length = 3, columnDefinition = "char(3)")
    private String currency;
}
