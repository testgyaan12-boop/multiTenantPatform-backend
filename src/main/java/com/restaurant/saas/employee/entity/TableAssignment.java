package com.restaurant.saas.employee.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "table_assignments")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class TableAssignment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "released_at")
    private Instant releasedAt;
}
