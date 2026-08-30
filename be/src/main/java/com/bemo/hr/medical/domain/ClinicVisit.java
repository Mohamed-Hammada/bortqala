package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_visits")
@Getter
@Setter
@NoArgsConstructor
public class ClinicVisit {

    public enum Status {
        WAITING,
        IN_ROOM,
        DONE,
        CANCELLED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "doctor_employee_id", length = 36, nullable = false)
    private String doctorEmployeeId;

    @Column(name = "visit_date", length = 10, nullable = false)
    private String visitDate;

    @Column(name = "visit_time", nullable = false)
    private Instant visitTime;

    @Column(name = "token", nullable = false)
    private Integer token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;

    @Column(name = "diagnosis_icd", length = 50)
    private String diagnosisIcd;

    @Column(name = "diagnosis_notes", length = 1000)
    private String diagnosisNotes;

    @Column(name = "fee_charged", precision = 12, scale = 2, nullable = false)
    private BigDecimal feeCharged;

    @Column(name = "insurance_covered", precision = 12, scale = 2, nullable = false)
    private BigDecimal insuranceCovered;

    @Column(name = "patient_share", precision = 12, scale = 2, nullable = false)
    private BigDecimal patientShare;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ClinicVisit(String patientId, String doctorEmployeeId, String visitDate,
                       Integer token, BigDecimal feeCharged, BigDecimal insuranceCovered,
                       String paymentMethod) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.doctorEmployeeId = doctorEmployeeId;
        this.visitDate = visitDate;
        this.visitTime = Instant.now();
        this.token = token;
        this.status = Status.WAITING;
        this.feeCharged = feeCharged != null ? feeCharged : BigDecimal.ZERO;
        this.insuranceCovered = insuranceCovered != null ? insuranceCovered : BigDecimal.ZERO;
        this.patientShare = this.feeCharged.subtract(this.insuranceCovered).max(BigDecimal.ZERO);
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }

    public void callToRoom() {
        this.status = Status.IN_ROOM;
        this.updatedAt = Instant.now();
    }

    public void complete(String chiefComplaint, String diagnosisIcd, String diagnosisNotes,
                         BigDecimal feeCharged, BigDecimal insuranceCovered, String paymentMethod) {
        this.status = Status.DONE;
        this.chiefComplaint = chiefComplaint;
        this.diagnosisIcd = diagnosisIcd;
        this.diagnosisNotes = diagnosisNotes;
        if (feeCharged != null) {
            this.feeCharged = feeCharged;
        }
        if (insuranceCovered != null) {
            this.insuranceCovered = insuranceCovered;
        }
        this.patientShare = this.feeCharged.subtract(this.insuranceCovered).max(BigDecimal.ZERO);
        if (paymentMethod != null) {
            this.paymentMethod = paymentMethod;
        }
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
