package com.restaurant.saas.audit.service;

import com.restaurant.saas.audit.entity.AuditLog;
import com.restaurant.saas.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(Long tenantId, Long actorId, String actorEmail, String action,
                       String entityType, String entityId, String before, String after) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setActorId(actorId);
        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBeforeState(before);
        log.setAfterState(after);
        auditLogRepository.save(log);
    }
}
