package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fiscal_periods")
public class FiscalPeriod {

    public enum Status {
        OPEN,
        CLOSED,
        LOCKED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "period_name", nullable = false, length = 100)
    private String periodName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "closed_at")
    private Long closedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected FiscalPeriod() {}

    public FiscalPeriod(int fiscalYear, int periodNumber, String periodName, LocalDate startDate, LocalDate endDate, Status status) {
        this.id = UUID.randomUUID().toString();
        this.fiscalYear = fiscalYear;
        this.periodNumber = periodNumber;
        this.periodName = periodName.strip();
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status == null ? Status.OPEN : status;
    }

    public void updateStatus(Status newStatus, String username) {
        this.status = newStatus;
        if (newStatus == Status.CLOSED || newStatus == Status.LOCKED) {
            this.closedBy = username;
            this.closedAt = System.currentTimeMillis();
        } else {
            this.closedBy = null;
            this.closedAt = null;
        }
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public int getFiscalYear() { return fiscalYear; }
    public int getPeriodNumber() { return periodNumber; }
    public String getPeriodName() { return periodName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Status getStatus() { return status; }
    public String getClosedBy() { return closedBy; }
    public Long getClosedAt() { return closedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
