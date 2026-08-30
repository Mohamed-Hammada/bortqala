package com.bemo.hr.security.pack.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_trusted_devices")
public class TrustedDevice {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "device_label", nullable = false)
    private String deviceLabel;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TrustedDevice() {
    }

    public TrustedDevice(String appId, String userId, String deviceId, String deviceLabel, String userAgent, String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceLabel = deviceLabel;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.lastSeenAt = Instant.now();
        this.revoked = false;
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

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void recordActivity(String deviceLabel, String userAgent, String ipAddress) {
        if (deviceLabel != null && !deviceLabel.isBlank()) {
            this.deviceLabel = deviceLabel;
        }
        if (userAgent != null && !userAgent.isBlank()) {
            this.userAgent = userAgent;
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            this.ipAddress = ipAddress;
        }
        this.lastSeenAt = Instant.now();
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
