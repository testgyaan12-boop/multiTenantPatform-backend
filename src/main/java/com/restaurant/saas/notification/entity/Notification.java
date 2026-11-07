package com.restaurant.saas.notification.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Notification extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String channel;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    private String subject;

    @Column(columnDefinition = "longtext")
    private String body;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(columnDefinition = "longtext")
    private String metadata;
}
