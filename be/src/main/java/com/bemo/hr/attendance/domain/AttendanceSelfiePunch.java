package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * WP-14 AC-3: employee self-service selfie punch. The server stamps {@code punchedAt};
 * the client-generated operation id makes replays (offline outbox retries) idempotent.
 */
@Entity
@Table(name = "attendance_selfie_punches")
public class AttendanceSelfiePunch {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;
    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;
    @Column(name = "operation_id", nullable = false, length = 64)
    private String operationId;
    @Column(name = "punched_at", nullable = false)
    private long punchedAt;
    @Column(name = "client_timestamp")
    private Long clientTimestamp;
    @Column(name = "image_content_type", length = 50)
    private String imageContentType;
    @Column(name = "image_data", columnDefinition = "TEXT")
    private String imageData;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected AttendanceSelfiePunch() {
    }

    public AttendanceSelfiePunch(String employeeId, String operationId, Long clientTimestamp,
                                 String imageContentType, String imageData) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.operationId = operationId;
        this.punchedAt = System.currentTimeMillis();
        this.clientTimestamp = clientTimestamp;
        this.imageContentType = imageContentType;
        this.imageData = imageData;
    }

    public boolean belongsTo(String employeeId) {
        return this.employeeId.equals(employeeId);
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
        if (punchedAt <= 0) punchedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getOperationId() {
        return operationId;
    }

    public long getPunchedAt() {
        return punchedAt;
    }

    public Long getClientTimestamp() {
        return clientTimestamp;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
