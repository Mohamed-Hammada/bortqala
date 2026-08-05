package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "quality_inspections")
public class QualityInspection {

    public enum Status {
        PASSED,
        FAILED,
        REJECTED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "inspection_number", nullable = false, length = 50)
    private String inspectionNumber;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "passed_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal passedQuantity;

    @Column(name = "failed_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal failedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "inspector_name", nullable = false, length = 100)
    private String inspectorName;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected QualityInspection() {}

    public QualityInspection(String inspectionNumber, LocalDate inspectionDate, String sourceType, BigDecimal passedQuantity, BigDecimal failedQuantity, Status status, String inspectorName, String notes) {
        this.id = UUID.randomUUID().toString();
        this.inspectionNumber = inspectionNumber.strip();
        this.inspectionDate = inspectionDate;
        this.sourceType = sourceType.strip();
        this.passedQuantity = passedQuantity == null ? BigDecimal.ZERO : passedQuantity;
        this.failedQuantity = failedQuantity == null ? BigDecimal.ZERO : failedQuantity;
        this.status = status == null ? Status.PASSED : status;
        this.inspectorName = inspectorName.strip();
        this.notes = notes == null ? null : notes.strip();
        this.createdAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() { if (createdAt == 0) createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getInspectionNumber() { return inspectionNumber; }
    public LocalDate getInspectionDate() { return inspectionDate; }
    public String getSourceType() { return sourceType; }
    public BigDecimal getPassedQuantity() { return passedQuantity; }
    public BigDecimal getFailedQuantity() { return failedQuantity; }
    public Status getStatus() { return status; }
    public String getInspectorName() { return inspectorName; }
    public String getNotes() { return notes; }
    public long getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
