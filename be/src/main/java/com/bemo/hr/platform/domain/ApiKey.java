package com.bemo.hr.platform.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"app_id", "key_hash"})
})
public class ApiKey {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;
    @Column(name = "scopes", nullable = false, length = 1000)
    private String scopes;
    @Column(name = "rate_limit_per_min", nullable = false)
    private int rateLimitPerMin = 120;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected ApiKey() {}

    public ApiKey(String appId, String name, String keyHash, String scopes, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.name = name;
        this.keyHash = keyHash;
        this.scopes = scopes;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public int getRateLimitPerMin() { return rateLimitPerMin; }
    public void setRateLimitPerMin(int rateLimitPerMin) { this.rateLimitPerMin = rateLimitPerMin; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public java.util.Set<String> scopeSet() {
        if (scopes == null || scopes.isBlank()) return java.util.Set.of();
        return java.util.Set.of(scopes.split(","));
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
