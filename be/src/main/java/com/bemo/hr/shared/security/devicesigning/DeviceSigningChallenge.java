package com.bemo.hr.shared.security.devicesigning;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_device_signing_challenges")
public class DeviceSigningChallenge {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "device_id", length = 36, nullable = false)
    private String deviceId;

    @Column(name = "nonce", length = 128, nullable = false)
    private String nonce;

    @Column(name = "operation_type", length = 60, nullable = false)
    private String operationType;

    @Column(name = "payload_hash", length = 64, nullable = false)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ChallengeStatus status = ChallengeStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeviceSigningChallenge() {
    }

    public DeviceSigningChallenge(String userId, String deviceId, String nonce, String operationType, String payloadHash, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = Objects.requireNonNull(userId, "userId must not be null").strip();
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId must not be null").strip();
        this.nonce = Objects.requireNonNull(nonce, "nonce must not be null").strip();
        this.operationType = Objects.requireNonNull(operationType, "operationType must not be null").strip();
        this.payloadHash = Objects.requireNonNull(payloadHash, "payloadHash must not be null").strip();
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = ChallengeStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markUsed() {
        this.status = ChallengeStatus.USED;
    }

    public void markExpired() {
        this.status = ChallengeStatus.EXPIRED;
    }

    public boolean isPending() {
        return this.status == ChallengeStatus.PENDING;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt) || this.status == ChallengeStatus.EXPIRED;
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

    public String getDeviceId() {
        return deviceId;
    }

    public String getNonce() {
        return nonce;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
