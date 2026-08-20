package com.bemo.hr.platform.deployment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenant_license_certificates")
public class TenantLicenseCertificate {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "license_key_hash", length = 64, nullable = false)
    private String licenseKeyHash;

    @Column(name = "certificate_payload", nullable = false, columnDefinition = "TEXT")
    private String certificatePayload;

    @Column(name = "signature_ed25519", length = 255, nullable = false)
    private String signatureEd25519;

    @Column(name = "device_fingerprint_hash", length = 64)
    private String deviceFingerprintHash;

    @Column(name = "licensed_seats", nullable = false)
    private int licensedSeats;

    @Column(name = "licensed_modules_json", nullable = false, columnDefinition = "TEXT")
    private String licensedModulesJson;

    @Column(name = "issue_date", nullable = false)
    private long issueDate;

    @Column(name = "expiry_date")
    private Long expiryDate;

    @Column(name = "is_perpetual", nullable = false)
    private boolean isPerpetual;

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays;

    @Column(name = "last_validated_at", nullable = false)
    private long lastValidatedAt;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // ACTIVE, EXPIRED, REVOKED, GRACE_PERIOD

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected TenantLicenseCertificate() {}

    public TenantLicenseCertificate(
            String licenseKeyHash,
            String certificatePayload,
            String signatureEd25519,
            String deviceFingerprintHash,
            int licensedSeats,
            String licensedModulesJson,
            long issueDate,
            Long expiryDate,
            boolean isPerpetual,
            int gracePeriodDays,
            long lastValidatedAt,
            String status,
            long timestamp) {
        this.id = UUID.randomUUID().toString();
        this.licenseKeyHash = Objects.requireNonNull(licenseKeyHash);
        this.certificatePayload = Objects.requireNonNull(certificatePayload);
        this.signatureEd25519 = Objects.requireNonNull(signatureEd25519);
        this.deviceFingerprintHash = deviceFingerprintHash;
        this.licensedSeats = licensedSeats;
        this.licensedModulesJson = Objects.requireNonNull(licensedModulesJson);
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.isPerpetual = isPerpetual;
        this.gracePeriodDays = gracePeriodDays;
        this.lastValidatedAt = lastValidatedAt;
        this.status = Objects.requireNonNull(status);
        this.createdAt = timestamp;
        this.updatedAt = timestamp;
    }

    public void updateValidationStatus(String newStatus, long timestamp) {
        this.status = Objects.requireNonNull(newStatus);
        this.lastValidatedAt = timestamp;
        this.updatedAt = timestamp;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getLicenseKeyHash() { return licenseKeyHash; }
    public String getCertificatePayload() { return certificatePayload; }
    public String getSignatureEd25519() { return signatureEd25519; }
    public String getDeviceFingerprintHash() { return deviceFingerprintHash; }
    public int getLicensedSeats() { return licensedSeats; }
    public String getLicensedModulesJson() { return licensedModulesJson; }
    public long getIssueDate() { return issueDate; }
    public Long getExpiryDate() { return expiryDate; }
    public boolean isPerpetual() { return isPerpetual; }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public long getLastValidatedAt() { return lastValidatedAt; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
