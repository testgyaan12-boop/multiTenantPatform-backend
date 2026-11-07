package com.restaurant.saas.audit.repository;

import com.restaurant.saas.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    java.util.List<AuditLog> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        Long tenantId, String entityType, String entityId);
}
