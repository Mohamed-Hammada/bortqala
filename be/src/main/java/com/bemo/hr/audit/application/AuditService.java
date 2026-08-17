package com.bemo.hr.audit.application;

import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String action, String entityType, String entityId, String username, String detailsJson, String ipAddress) {
        log.debug("record called with action={}, entityType={}, entityId={}, username={}", action, entityType, entityId, username);
        AuditLog auditLog = new AuditLog(action, entityType, entityId, username, detailsJson, ipAddress);
        auditLogRepository.save(auditLog);
        log.info("Audit log {} {} {} recorded for {}", action, entityType, entityId, username);
    }
}
