package com.bemo.hr.security.pack.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_user_totp_backup_codes")
public class UserTotpBackupCode {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserTotpBackupCode() {
    }

    public UserTotpBackupCode(String appId, String userId, String codeHash) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.userId = userId;
        this.codeHash = codeHash;
        this.used = false;
        this.createdAt = Instant.now();
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

    public String getCodeHash() {
        return codeHash;
    }

    public boolean isUsed() {
        return used;
    }

    public void markUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
