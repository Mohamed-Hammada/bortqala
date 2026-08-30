package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_allergies")
@Getter
@Setter
@NoArgsConstructor
public class PatientAllergy {

    public enum Severity {
        MILD,
        MODERATE,
        SEVERE
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "substance", length = 255, nullable = false)
    private String substance;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 32, nullable = false)
    private Severity severity = Severity.MODERATE;

    @Column(name = "reaction_notes", length = 1000)
    private String reactionNotes;

    @Column(name = "noted_at", nullable = false)
    private long notedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PatientAllergy(String patientId, String substance, Severity severity, String reactionNotes) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.substance = substance;
        this.severity = severity != null ? severity : Severity.MODERATE;
        this.reactionNotes = reactionNotes;
        this.notedAt = Instant.now().toEpochMilli();
        this.createdAt = this.notedAt;
        this.updatedAt = this.notedAt;
        this.version = 0L;
    }
}
