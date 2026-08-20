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
    private final com.bemo.hr.audit.application.AuditService auditService;

    public AuditLogController(AuditLogRepository repository) {
        this(repository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuditLogController(AuditLogRepository repository, com.bemo.hr.audit.application.AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("@auth.hasPermission('audit.read')")
    public AuditLogApi.AuditLogPageResponse listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean isBreakGlass,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to
    ) {
        var pageable = PageRequest.of(page, Math.min(size, 200));
        Page<AuditLog> result = isBreakGlass != null
                ? repository.searchWithBreakGlass(blankToNull(entityType), blankToNull(action), blankToNull(username), isBreakGlass, blankToNull(search), from, to, pageable)
                : repository.search(blankToNull(entityType), blankToNull(action), blankToNull(username), blankToNull(search), from, to, pageable);

        var content = result.getContent().stream().map(this::toResponse).toList();
        return new AuditLogApi.AuditLogPageResponse(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @org.springframework.web.bind.annotation.PostMapping("/break-glass")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public AuditLogApi.AuditLogResponse recordBreakGlass(
            @org.springframework.web.bind.annotation.RequestBody AuditLogApi.BreakGlassRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        auditService.recordBreakGlass(request.action(), request.entityType(), request.entityId(),
                username, request.reason(), request.detailsJson(), ip, userAgent);
        return new AuditLogApi.AuditLogResponse(
                java.util.UUID.randomUUID().toString(),
                request.action(),
                request.entityType(),
                request.entityId(),
                username,
                request.detailsJson(),
                ip,
                request.reason(),
                true,
                userAgent,
                System.currentTimeMillis()
        );
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
                log.getReason(),
                log.isBreakGlass(),
                log.getUserAgent(),
                log.getOccurredAt()
        );
    }
}
