package com.bemo.hr.workforce.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_settlement_snapshots")
public class WorkforceSettlementSnapshot {

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
    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;
    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;
    @Column(name = "frozen_at", nullable = false)
    private long frozenAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.FROZEN;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceSettlementSnapshot() {
    }

    public WorkforceSettlementSnapshot(String contractorId, String periodId, BigDecimal totalHours, BigDecimal grossAmount, BigDecimal netAmount) {
        this.id = UUID.randomUUID().toString();
        this.contractorId = contractorId;
        this.periodId = periodId;
        this.totalHours = totalHours;
        this.grossAmount = grossAmount;
        this.netAmount = netAmount;
        this.frozenAt = System.currentTimeMillis();
        this.status = Status.FROZEN;
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

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public long getFrozenAt() {
        return frozenAt;
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
        FROZEN
    }
}
