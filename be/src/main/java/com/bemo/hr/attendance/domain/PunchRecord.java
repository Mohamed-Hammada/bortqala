package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "punch_records")
public class PunchRecord {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    @Column(name = "device_id", length = 36)
    private String deviceId;
    @Column(name = "source_id", nullable = false, length = 36)
    private String sourceId;
    @Column(name = "employee_id")
    private String employeeId;
    @Column(name = "device_user_id", nullable = false, length = 100)
    private String deviceUserId;
    @Column(name = "raw_name", length = 200)
    private String rawName;
    @Column(name = "punched_at", nullable = false)
    private Instant punchedAt;
    @Column(name = "raw_line", nullable = false, columnDefinition = "TEXT")
    private String rawLine;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    protected PunchRecord() {
    }

    public PunchRecord(String batchId, String deviceId, String sourceId, String employeeId, String deviceUserId,
                       String rawName, Instant punchedAt, String rawLine, int rowNumber) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.deviceId = deviceId;
        this.sourceId = sourceId;
        this.employeeId = employeeId;
        this.deviceUserId = deviceUserId;
        this.rawName = rawName;
        this.punchedAt = punchedAt;
        this.rawLine = rawLine;
        this.rowNumber = rowNumber;
    }

    public String getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public String getRawName() {
        return rawName;
    }

    public Instant getPunchedAt() {
        return punchedAt;
    }

    public String getRawLine() {
        return rawLine;
    }

    public int getRowNumber() {
        return rowNumber;
    }
}
