package com.bemo.hr.audit.api;

import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogRepository repository;

    public AuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public AuditLogApi.AuditLogPageResponse listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to
    ) {
        var pageable = PageRequest.of(page, Math.min(size, 200));
        Page<AuditLog> result = repository.search(
                blankToNull(entityType),
                blankToNull(action),
                blankToNull(username),
                blankToNull(search),
                from,
                to,
                pageable);

        var content = result.getContent().stream().map(this::toResponse).toList();
        return new AuditLogApi.AuditLogPageResponse(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private AuditLogApi.AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogApi.AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getUsername(),
                log.getDetailsJson(),
                log.getIpAddress(),
                log.getOccurredAt()
        );
    }
}
