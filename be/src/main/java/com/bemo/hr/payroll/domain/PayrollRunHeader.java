package com.bemo.hr.payroll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_run_headers")
public class PayrollRunHeader {

    public enum Status {
        DRAFT, CALCULATED, APPROVED, POSTED, CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "run_number", nullable = false, length = 50)
    private String runNumber;

    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "total_gross", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "total_net", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PayrollRunHeader() {}

    public PayrollRunHeader(String runNumber, String periodId, LocalDate runDate) {
        this.id = UUID.randomUUID().toString();
        this.runNumber = runNumber;
        this.periodId = periodId;
        this.runDate = runDate;
        this.status = Status.DRAFT;
    }

    public void updateTotals(BigDecimal gross, BigDecimal deductions, BigDecimal net) {
        this.totalGross = gross;
        this.totalDeductions = deductions;
        this.totalNet = net;
        this.status = Status.CALCULATED;
    }

    public void approve() {
        if (this.status != Status.CALCULATED) {
            throw new IllegalStateException("Only CALCULATED payroll runs can be approved");
        }
        this.status = Status.APPROVED;
    }

    public void post() {
        if (this.status != Status.APPROVED) {
            throw new IllegalStateException("Only APPROVED payroll runs can be posted");
        }
        this.status = Status.POSTED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRunNumber() { return runNumber; }
    public String getPeriodId() { return periodId; }
    public LocalDate getRunDate() { return runDate; }
    public BigDecimal getTotalGross() { return totalGross; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public BigDecimal getTotalNet() { return totalNet; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
