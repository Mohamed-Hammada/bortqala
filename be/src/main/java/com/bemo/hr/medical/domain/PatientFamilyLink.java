package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "patient_family_links")
public class PatientFamilyLink {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "guardian_patient_id", length = 36, nullable = false)
    private String guardianPatientId;

    @Column(name = "relationship_type", length = 32, nullable = false)
    private String relationshipType; // PARENT, SPOUSE, SIBLING, CHILD, GUARDIAN

    @Column(name = "is_primary_payer", nullable = false)
    private boolean isPrimaryPayer;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public PatientFamilyLink() {}

    public PatientFamilyLink(
            String id,
            String tenantId,
            String patientId,
            String guardianPatientId,
            String relationshipType,
            boolean isPrimaryPayer,
            String notes) {
        this.id = id;
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.guardianPatientId = guardianPatientId;
        this.relationshipType = relationshipType;
        this.isPrimaryPayer = isPrimaryPayer;
        this.notes = notes;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getGuardianPatientId() {
        return guardianPatientId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public boolean isPrimaryPayer() {
        return isPrimaryPayer;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
