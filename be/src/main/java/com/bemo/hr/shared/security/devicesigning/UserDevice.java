package com.bemo.hr.shared.security.devicesigning;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_user_devices")
public class UserDevice {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "device_identifier", length = 100, nullable = false)
    private String deviceIdentifier;

    @Column(name = "device_name", length = 150, nullable = false)
    private String deviceName;

    @Column(name = "public_key", length = 2048, nullable = false)
    private String publicKey;

    @Column(name = "algorithm", length = 50, nullable = false)
    private String algorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserDevice() {
    }

    public UserDevice(String userId, String deviceIdentifier, String deviceName, String publicKey, String algorithm) {
        this.id = UUID.randomUUID().toString();
        this.userId = Objects.requireNonNull(userId, "userId must not be null").strip();
        this.deviceIdentifier = Objects.requireNonNull(deviceIdentifier, "deviceIdentifier must not be null").strip();
        this.deviceName = Objects.requireNonNull(deviceName, "deviceName must not be null").strip();
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey must not be null").strip();
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm must not be null").strip();
        this.status = DeviceStatus.ACTIVE;
        this.enrolledAt = Instant.now();
        this.createdAt = this.enrolledAt;
        this.updatedAt = this.enrolledAt;
    }

    public void revoke(String reason) {
        this.status = DeviceStatus.REVOKED;
        this.revokedReason = reason != null ? reason.strip() : "REVOKED_BY_USER";
        this.revokedAt = Instant.now();
        this.updatedAt = this.revokedAt;
    }

    public void recordUsage() {
        this.lastUsedAt = Instant.now();
        this.updatedAt = this.lastUsedAt;
    }

    public boolean isActive() {
        return this.status == DeviceStatus.ACTIVE;
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

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
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
