package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_admissions")
@Getter
@Setter
@NoArgsConstructor
public class HospitalAdmission {

    public enum Status {
        ADMITTED, DISCHARGED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "admitting_doctor_id", length = 36, nullable = false)
    private String admittingDoctorId;

    @Column(name = "current_bed_id", length = 36)
    private String currentBedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.ADMITTED;

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;

    @Column(name = "admitted_at", nullable = false)
    private long admittedAt;

    @Column(name = "discharged_at")
    private Long dischargedAt;

    @Column(name = "discharge_summary", length = 2000)
    private String dischargeSummary;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalAdmission(String patientId, String admittingDoctorId, String currentBedId, String chiefComplaint) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.admittingDoctorId = admittingDoctorId;
        this.currentBedId = currentBedId;
        this.chiefComplaint = chiefComplaint;
        this.status = Status.ADMITTED;
        this.admittedAt = Instant.now().toEpochMilli();
        this.createdAt = this.admittedAt;
        this.updatedAt = this.createdAt;
    }

    public void transferBed(String newBedId) {
        this.currentBedId = newBedId;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void discharge(String summary) {
        this.status = Status.DISCHARGED;
        this.dischargedAt = Instant.now().toEpochMilli();
        this.dischargeSummary = summary;
        this.currentBedId = null;
        this.updatedAt = this.dischargedAt;
    }
}
