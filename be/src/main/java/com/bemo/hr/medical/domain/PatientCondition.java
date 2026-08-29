package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_conditions")
@Getter
@Setter
@NoArgsConstructor
public class PatientCondition {

    public enum Status {
        ACTIVE,
        RESOLVED,
        INACTIVE
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "icd_code", length = 32)
    private String icdCode;

    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Column(name = "is_chronic", nullable = false)
    private boolean chronic = false;

    @Column(name = "onset_date", length = 32)
    private String onsetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PatientCondition(String patientId, String icdCode, String label, boolean chronic, String onsetDate, Status status, String notes) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.icdCode = icdCode;
        this.label = label;
        this.chronic = chronic;
        this.onsetDate = onsetDate;
        this.status = status != null ? status : Status.ACTIVE;
        this.notes = notes;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }
}
