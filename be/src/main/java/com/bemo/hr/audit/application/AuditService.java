package com.bemo.hr.audit.application;

import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String action, String entityType, String entityId, String username, String detailsJson, String ipAddress) {
        AuditLog log = new AuditLog(action, entityType, entityId, username, detailsJson, ipAddress);
        auditLogRepository.save(log);
    }
}
