package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "telemedicine_sessions")
public class TelemedicineSession {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "doctor_id", length = 36, nullable = false)
    private String doctorId;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "scheduled_time", nullable = false)
    private long scheduledTime;

    @Column(name = "meeting_link", length = 500, nullable = false)
    private String meetingLink;

    @Column(name = "room_token", length = 128)
    private String roomToken;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "clinical_notes")
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public TelemedicineSession() {}

    public TelemedicineSession(
            String id,
            String tenantId,
            String patientId,
            String doctorId,
            String doctorName,
            long scheduledTime,
            String meetingLink,
            String roomToken) {
        this.id = id;
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.scheduledTime = scheduledTime;
        this.meetingLink = meetingLink;
        this.roomToken = roomToken;
        this.status = "SCHEDULED";
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

    public String getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public String getRoomToken() {
        return roomToken;
    }

    public String getStatus() {
        return status;
    }

    public void start() {
        this.status = "IN_PROGRESS";
        this.updatedAt = System.currentTimeMillis();
    }

    public void complete(String notes) {
        this.status = "COMPLETED";
        this.clinicalNotes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.updatedAt = System.currentTimeMillis();
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
