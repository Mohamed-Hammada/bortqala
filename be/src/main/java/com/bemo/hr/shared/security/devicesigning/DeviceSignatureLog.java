package com.bemo.hr.shared.security.devicesigning;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_device_signature_logs")
public class DeviceSignatureLog {

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

    @Column(name = "challenge_id", length = 36, nullable = false)
    private String challengeId;

    @Column(name = "operation_type", length = 60, nullable = false)
    private String operationType;

    @Column(name = "signature_value", length = 1024, nullable = false)
    private String signatureValue;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    protected DeviceSignatureLog() {
    }

    public DeviceSignatureLog(String userId, String deviceId, String challengeId, String operationType, String signatureValue, String status) {
        this.id = UUID.randomUUID().toString();
        this.userId = Objects.requireNonNull(userId, "userId must not be null").strip();
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId must not be null").strip();
        this.challengeId = Objects.requireNonNull(challengeId, "challengeId must not be null").strip();
        this.operationType = Objects.requireNonNull(operationType, "operationType must not be null").strip();
        this.signatureValue = Objects.requireNonNull(signatureValue, "signatureValue must not be null").strip();
        this.status = Objects.requireNonNull(status, "status must not be null").strip();
        this.verifiedAt = Instant.now();
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

    public String getChallengeId() {
        return challengeId;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getSignatureValue() {
        return signatureValue;
    }

    public String getStatus() {
        return status;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
