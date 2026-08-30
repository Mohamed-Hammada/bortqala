package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "medical_license_registry")
public class MedicalLicenseRecord {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "practitioner_id", length = 36, nullable = false)
    private String practitionerId;

    @Column(name = "practitioner_name", nullable = false)
    private String practitionerName;

    @Column(name = "license_type", length = 64, nullable = false)
    private String licenseType; // PHYSICIAN, NURSE, PHARMACIST, DENTIST, LAB_SPECIALIST

    @Column(name = "license_number", length = 64, nullable = false)
    private String licenseNumber;

    @Column(name = "issuing_authority", length = 128, nullable = false)
    private String issuingAuthority; // MOH, SYNDICATE, GAHAR

    @Column(name = "issue_date", nullable = false)
    private long issueDate;

    @Column(name = "expiry_date", nullable = false)
    private long expiryDate;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // VALID, EXPIRED, EXPIRING_SOON

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public MedicalLicenseRecord() {}

    public MedicalLicenseRecord(
            String id,
            String tenantId,
            String practitionerId,
            String practitionerName,
            String licenseType,
            String licenseNumber,
            String issuingAuthority,
            long issueDate,
            long expiryDate) {
        this.id = id;
        this.tenantId = tenantId;
        this.practitionerId = practitionerId;
        this.practitionerName = practitionerName;
        this.licenseType = licenseType;
        this.licenseNumber = licenseNumber;
        this.issuingAuthority = issuingAuthority;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.status = computeStatus(expiryDate);
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    private static String computeStatus(long expiryDate) {
        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
        if (expiryDate < now) {
            return "EXPIRED";
        } else if (expiryDate - now <= thirtyDaysMs) {
            return "EXPIRING_SOON";
        } else {
            return "VALID";
        }
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getPractitionerId() {
        return practitionerId;
    }

    public String getPractitionerName() {
        return practitionerName;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public long getIssueDate() {
        return issueDate;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public String getStatus() {
        return computeStatus(this.expiryDate);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
