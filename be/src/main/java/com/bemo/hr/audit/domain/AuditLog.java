package com.bemo.hr.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false)
    private long occurredAt;

    protected AuditLog() {}

    public AuditLog(String action, String entityType, String entityId, String username, String detailsJson, String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.action = action.strip();
        this.entityType = entityType.strip();
        this.entityId = entityId == null ? null : entityId.strip();
        this.username = username == null ? "SYSTEM" : username.strip();
        this.detailsJson = detailsJson;
        this.ipAddress = ipAddress;
        this.occurredAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() {
        if (occurredAt == 0) occurredAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getUsername() { return username; }
    public String getDetailsJson() { return detailsJson; }
    public String getIpAddress() { return ipAddress; }
    public long getOccurredAt() { return occurredAt; }
}
