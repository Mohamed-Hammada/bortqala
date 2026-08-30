package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_dispense_records")
@Getter
@Setter
@NoArgsConstructor
public class PharmacyDispenseRecord {

    public enum Status {
        PENDING_APPROVAL,
        DISPENSED,
        REJECTED
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "prescription_id", length = 64, nullable = false)
    private String prescriptionId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "prescriber_doctor_id", length = 64, nullable = false)
    private String prescriberDoctorId;

    @Column(name = "dispenser_user_id", length = 64, nullable = false)
    private String dispenserUserId;

    @Column(name = "second_signer_id", length = 64)
    private String secondSignerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status = Status.DISPENSED;

    @Column(name = "is_controlled", nullable = false)
    private boolean controlled = false;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PharmacyDispenseRecord(String prescriptionId,
                                  String patientId,
                                  String prescriberDoctorId,
                                  String dispenserUserId,
                                  String secondSignerId,
                                  Status status,
                                  boolean controlled,
                                  String notes) {
        this.id = UUID.randomUUID().toString();
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.prescriberDoctorId = prescriberDoctorId;
        this.dispenserUserId = dispenserUserId;
        this.secondSignerId = secondSignerId;
        this.status = status != null ? status : Status.DISPENSED;
        this.controlled = controlled;
        this.notes = notes;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }

    public void approve(String secondSignerId) {
        this.secondSignerId = secondSignerId;
        this.status = Status.DISPENSED;
        this.updatedAt = Instant.now().toEpochMilli();
    }
}
