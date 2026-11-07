package com.restaurant.saas.customer.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_sessions")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class GuestSession extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "session_token", nullable = false, length = 36, columnDefinition = "char(36)")
    private String sessionToken = UUID.randomUUID().toString();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
