package com.bemo.hr.compliance.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "retention_policies")
public class RetentionPolicy {

    public enum Action { ANONYMIZE, DELETE }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(nullable = false)
    private int months;
    @Column(nullable = false, length = 20)
    private String action;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected RetentionPolicy() {}

    public RetentionPolicy(String appId, String entityKey, int months, Action action) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.entityKey = entityKey;
        this.months = months;
        this.action = action.name();
        this.active = true;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getEntityKey() { return entityKey; }
    public int getMonths() { return months; }
    public Action getAction() { return Action.valueOf(action); }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }

    public void setMonths(int months) { this.months = months; }
    public void setAction(Action action) { this.action = action.name(); }
    public void setActive(boolean active) { this.active = active; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }
}
