package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_appointments")
@Getter
@Setter
@NoArgsConstructor
public class ClinicAppointment {

    public enum Status {
        BOOKED,
        CONFIRMED,
        CHECKED_IN,
        NO_SHOW,
        CANCELLED,
        DONE
    }

    public enum Source {
        WALKIN,
        PHONE,
        ONLINE,
        WHATSAPP
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "doctor_employee_id", length = 64, nullable = false)
    private String doctorEmployeeId;

    @Column(name = "visit_date", length = 16, nullable = false)
    private String visitDate; // "2026-08-30"

    @Column(name = "start_time", length = 8, nullable = false)
    private String startTime; // "10:00"

    @Column(name = "starts_at", nullable = false)
    private long startsAt; // epoch millis

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 20;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status = Status.BOOKED;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 32, nullable = false)
    private Source source = Source.PHONE;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "clinic_visit_id", length = 64)
    private String clinicVisitId;

    @Column(name = "reminder_sent_at")
    private Long reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ClinicAppointment(String patientId,
                             String doctorEmployeeId,
                             String visitDate,
                             String startTime,
                             long startsAt,
                             int durationMinutes,
                             Source source,
                             String reason) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.doctorEmployeeId = doctorEmployeeId;
        this.visitDate = visitDate;
        this.startTime = startTime;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes > 0 ? durationMinutes : 20;
        this.status = Status.BOOKED;
        this.source = source != null ? source : Source.PHONE;
        this.reason = reason;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }

    public void confirm() {
        this.status = Status.CONFIRMED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void checkIn(String clinicVisitId) {
        this.status = Status.CHECKED_IN;
        this.clinicVisitId = clinicVisitId;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void markNoShow() {
        this.status = Status.NO_SHOW;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void markDone() {
        this.status = Status.DONE;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void markReminderSent() {
        this.reminderSentAt = Instant.now().toEpochMilli();
        this.updatedAt = this.reminderSentAt;
    }
}
