package com.bemo.hr.security.pack.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_user_totp")
public class UserTotp {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "secret_encrypted", nullable = false)
    private String secretEncrypted;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "enabled_at")
    private Instant enabledAt;

    @Column(name = "last_used_step", nullable = false)
    private long lastUsedStep;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserTotp() {
    }

    public UserTotp(String appId, String userId, String secretEncrypted) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.userId = userId;
        this.secretEncrypted = secretEncrypted;
        this.enabled = false;
        this.lastUsedStep = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public void setSecretEncrypted(String secretEncrypted) {
        this.secretEncrypted = secretEncrypted;
        this.updatedAt = Instant.now();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
        this.enabledAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.enabledAt = null;
        this.updatedAt = Instant.now();
    }

    public Instant getEnabledAt() {
        return enabledAt;
    }

    public long getLastUsedStep() {
        return lastUsedStep;
    }

    public void setLastUsedStep(long lastUsedStep) {
        this.lastUsedStep = lastUsedStep;
        this.updatedAt = Instant.now();
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
