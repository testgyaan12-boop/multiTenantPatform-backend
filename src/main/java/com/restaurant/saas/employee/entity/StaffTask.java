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
@Table(name = "tasks")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class StaffTask extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "task_type", nullable = false, length = 40)
    private String taskType = "CUSTOM_TASK";

    @Column(nullable = false, length = 12)
    private String priority = "NORMAL";

    @Column(nullable = false, length = 20)
    private String status = "ASSIGNED";

    @Column(name = "assigned_to_employee_id")
    private Long assignedToEmployeeId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(length = 1000)
    private String instructions;

    @Column(name = "status_note", length = 300)
    private String statusNote;
}
