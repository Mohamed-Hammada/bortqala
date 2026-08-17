package com.bemo.hr.finance.domain.close;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "period_close_execution_records")
public class PeriodCloseExecutionRecord {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;
    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "blocker_reason", length = 255)
    private String blockerReason;
    @Column(name = "closed_at")
    private Long closedAt;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PeriodCloseExecutionRecord() {
    }

    public PeriodCloseExecutionRecord(String periodId, String moduleName, Status status, String blockerReason) {
        this.id = UUID.randomUUID().toString();
        this.periodId = periodId;
        this.moduleName = moduleName;
        this.status = status;
        this.blockerReason = blockerReason;
        if (status == Status.CLOSED) {
            this.closedAt = System.currentTimeMillis();
        }
    }

    public void markClosed() {
        this.status = Status.CLOSED;
        this.closedAt = System.currentTimeMillis();
        this.blockerReason = null;
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

    public String getPeriodId() {
        return periodId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Status getStatus() {
        return status;
    }

    public String getBlockerReason() {
        return blockerReason;
    }

    public Long getClosedAt() {
        return closedAt;
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
        READY, CLOSED, BLOCKED
    }
}
