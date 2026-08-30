package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_ot_schedules")
@Getter
@Setter
@NoArgsConstructor
public class HospitalOtSchedule {

    public enum Status {
        PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "theater_name", length = 100, nullable = false)
    private String theaterName;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "surgeon_doctor_id", length = 36, nullable = false)
    private String surgeonDoctorId;

    @Column(name = "surgery_type", length = 200, nullable = false)
    private String surgeryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.PLANNED;

    @Column(name = "planned_start", nullable = false)
    private long plannedStart;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "actual_start")
    private Long actualStart;

    @Column(name = "actual_end")
    private Long actualEnd;

    @Column(name = "anesthesia_notes", length = 1000)
    private String anesthesiaNotes;

    @Column(name = "surgical_notes", length = 2000)
    private String surgicalNotes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalOtSchedule(String theaterName, String patientId, String surgeonDoctorId,
                              String surgeryType, long plannedStart, int durationMinutes) {
        this.id = UUID.randomUUID().toString();
        this.theaterName = theaterName;
        this.patientId = patientId;
        this.surgeonDoctorId = surgeonDoctorId;
        this.surgeryType = surgeryType;
        this.plannedStart = plannedStart;
        this.durationMinutes = durationMinutes > 0 ? durationMinutes : 60;
        this.status = Status.PLANNED;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public void startSurgery() {
        this.status = Status.IN_PROGRESS;
        this.actualStart = Instant.now().toEpochMilli();
        this.updatedAt = this.actualStart;
    }

    public void completeSurgery(String anesthesiaNotes, String surgicalNotes) {
        this.status = Status.COMPLETED;
        this.actualEnd = Instant.now().toEpochMilli();
        this.anesthesiaNotes = anesthesiaNotes;
        this.surgicalNotes = surgicalNotes;
        this.updatedAt = this.actualEnd;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.updatedAt = Instant.now().toEpochMilli();
    }
}
