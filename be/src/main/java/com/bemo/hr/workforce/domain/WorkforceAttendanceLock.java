package com.bemo.hr.workforce.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_attendance_locks")
public class WorkforceAttendanceLock {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;
    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;
    @Column(name = "total_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHours;
    @Column(name = "locked_by", nullable = false, length = 100)
    private String lockedBy;
    @Column(name = "locked_at", nullable = false)
    private long lockedAt;
    @Column(name = "correction_reason", length = 255)
    private String correctionReason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.LOCKED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceAttendanceLock() {
    }

    public WorkforceAttendanceLock(String contractorId, String periodId, BigDecimal totalHours, String lockedBy) {
        this.id = UUID.randomUUID().toString();
        this.contractorId = contractorId;
        this.periodId = periodId;
        this.totalHours = totalHours;
        this.lockedBy = lockedBy;
        this.lockedAt = System.currentTimeMillis();
        this.status = Status.LOCKED;
    }

    public void correct(BigDecimal newTotalHours, String reason) {
        this.totalHours = newTotalHours;
        this.correctionReason = reason;
        this.status = Status.CORRECTED;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getContractorId() {
        return contractorId;
    }

    public String getPeriodId() {
        return periodId;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public long getLockedAt() {
        return lockedAt;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        LOCKED, CORRECTED
    }
}
