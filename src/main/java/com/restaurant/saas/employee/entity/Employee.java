package com.restaurant.saas.employee.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Employee extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "employee_code", nullable = false, length = 40)
    private String employeeCode;

    @Column(name = "job_title", length = 80)
    private String jobTitle;

    @Column(name = "hired_at")
    private LocalDate hiredAt;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "shift_name", length = 60)
    private String shiftName;

    @Column(name = "on_duty", nullable = false)
    private Boolean onDuty = Boolean.FALSE;

    @Column(name = "on_duty_since")
    private Instant onDutySince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private com.restaurant.saas.identity.entity.User user;
}
