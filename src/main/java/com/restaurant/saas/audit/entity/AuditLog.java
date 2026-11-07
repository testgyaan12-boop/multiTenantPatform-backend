package com.restaurant.saas.audit.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "audit_logs")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class AuditLog extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "before_state", columnDefinition = "longtext")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "longtext")
    private String afterState;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "request_id")
    private String requestId;
}
