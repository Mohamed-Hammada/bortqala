package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    @Column(name = "replaced_by_token_id", length = 36)
    private String replacedByTokenId;

    protected RefreshToken() {
    }

    public RefreshToken(String appId, String userId, String tokenHash, Instant expiresAt, String deviceId) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.deviceId = deviceId;
    }

    public void revoke(String by) {
        this.revokedAt = Instant.now();
        this.revokedBy = by;
    }

    public void markReplacedBy(String replacementTokenId) {
        this.replacedByTokenId = replacementTokenId;
        this.revokedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getDeviceId() { return deviceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevokedBy() { return revokedBy; }
    public String getReplacedByTokenId() { return replacedByTokenId; }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
