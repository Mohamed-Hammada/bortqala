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
        record(action, entityType, entityId, username, detailsJson, ipAddress, null, false, null);
    }

    @Transactional
    public void record(String action, String entityType, String entityId, String username, String detailsJson,
                       String ipAddress, String reason, boolean isBreakGlass, String userAgent) {
        log.debug("record called with action={}, entityType={}, entityId={}, username={}, isBreakGlass={}",
                action, entityType, entityId, username, isBreakGlass);
        AuditLog auditLog = new AuditLog(action, entityType, entityId, username, detailsJson, ipAddress, reason, isBreakGlass, userAgent);
        auditLogRepository.save(auditLog);
        log.info("Audit log {} {} {} (breakGlass={}) recorded for {}", action, entityType, entityId, isBreakGlass, username);
    }

    @Transactional
    public void recordBreakGlass(String action, String entityType, String entityId, String username,
                                 String reason, String detailsJson, String ipAddress, String userAgent) {
        log.warn("BREAK-GLASS action {} on {}/{} executed by {} with reason: {}",
                action, entityType, entityId, username, reason);
        record(action, entityType, entityId, username, detailsJson, ipAddress, reason, true, userAgent);
    }
}
