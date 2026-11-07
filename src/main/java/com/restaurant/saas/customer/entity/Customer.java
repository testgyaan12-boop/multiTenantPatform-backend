package com.restaurant.saas.customer.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "customers")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    @Column(name = "marketing_opt_in", nullable = false)
    private Boolean marketingOptIn = Boolean.FALSE;
}
