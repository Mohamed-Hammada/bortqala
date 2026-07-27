package com.bemo.hr.audit.api;

import java.util.List;

public class AuditLogApi {

    public record AuditLogResponse(
            String id,
            String action,
            String entityType,
            String entityId,
            String username,
            String detailsJson,
            String ipAddress,
            long occurredAt
    ) {}

    public record AuditLogPageResponse(
            List<AuditLogResponse> content,
            int page,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
