package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_mar_entries")
@Getter
@Setter
@NoArgsConstructor
public class HospitalMarEntry {

    public enum Status {
        DUE, GIVEN, REFUSED, HELD
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "admission_id", length = 36, nullable = false)
    private String admissionId;

    @Column(name = "medication_name", length = 160, nullable = false)
    private String medicationName;

    @Column(name = "dose", length = 60, nullable = false)
    private String dose;

    @Column(name = "route", length = 30, nullable = false)
    private String route = "ORAL";

    @Column(name = "due_at", nullable = false)
    private long dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.DUE;

    @Column(name = "administered_at")
    private Long administeredAt;

    @Column(name = "nurse_id", length = 36)
    private String nurseId;

    @Column(name = "nurse_name", length = 160)
    private String nurseName;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalMarEntry(String admissionId, String medicationName, String dose, String route, long dueAt) {
        this.id = UUID.randomUUID().toString();
        this.admissionId = admissionId;
        this.medicationName = medicationName;
        this.dose = dose;
        this.route = route != null ? route : "ORAL";
        this.dueAt = dueAt;
        this.status = Status.DUE;
        this.createdAt = Instant.now().toEpochMilli();
    }

    public void administer(Status newStatus, String nurseId, String nurseName, String notes) {
        this.status = newStatus != null ? newStatus : Status.GIVEN;
        this.administeredAt = Instant.now().toEpochMilli();
        this.nurseId = nurseId;
        this.nurseName = nurseName;
        this.notes = notes;
    }
}
