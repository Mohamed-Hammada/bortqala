package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_import_rows")
@Getter
public class WorkforceImportRow {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @Column(name = "raw_data", nullable = false, length = 4000)
    private String rawData;
    @Column(name = "worker_code", length = 50)
    private String workerCode;
    @Column(name = "worker_id", length = 36)
    private String workerId;
    @Column(name = "work_date", length = 10)
    private String workDate;
    @Column(name = "attendance_value", precision = 4, scale = 2)
    private BigDecimal attendanceValue;
    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;
    @Column(name = "error_code", length = 60)
    private String errorCode;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkforceImportRow() {
    }

    public WorkforceImportRow(String batchId, int rowNumber, String rawData, String workerCode, String workerId,
                              String workDate, BigDecimal attendanceValue, String validationStatus,
                              String errorCode, String errorMessage) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.rowNumber = rowNumber;
        this.rawData = rawData;
        this.workerCode = workerCode;
        this.workerId = workerId;
        this.workDate = workDate;
        this.attendanceValue = attendanceValue;
        this.validationStatus = validationStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
