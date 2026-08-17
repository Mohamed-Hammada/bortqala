package com.bemo.hr.payroll.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_run_headers")
public class PayrollRunHeader {

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

    protected PayrollRunHeader() {
    }

    public PayrollRunHeader(String runNumber, String periodId, LocalDate runDate) {
        this.id = UUID.randomUUID().toString();
        this.runNumber = runNumber;
        this.periodId = periodId;
        this.runDate = runDate;
        this.status = Status.DRAFT;
    }

    public void updateTotals(BigDecimal gross, BigDecimal deductions, BigDecimal net) {
        if (this.status != Status.DRAFT && this.status != Status.CALCULATED) {
            throw invalidTransition(Status.CALCULATED);
        }
        this.totalGross = gross;
        this.totalDeductions = deductions;
        this.totalNet = net;
        this.status = Status.CALCULATED;
    }

    public void transitionTo(Status nextStatus) {
        Status expected = switch (this.status) {
            case CALCULATED -> Status.REVIEWED;
            case REVIEWED -> Status.APPROVED;
            case APPROVED -> Status.POSTED;
            case POSTED -> Status.PAID;
            default -> null;
        };
        if (nextStatus == null || nextStatus != expected) {
            throw invalidTransition(nextStatus);
        }
        this.status = nextStatus;
    }

    public void approve() {
        transitionTo(Status.APPROVED);
    }

    public void post() {
        transitionTo(Status.POSTED);
    }

    public void reopenAfterPaymentReversal() {
        if (this.status == Status.POSTED) return;
        if (this.status != Status.PAID) {
            throw invalidTransition(Status.POSTED);
        }
        this.status = Status.POSTED;
    }

    private BusinessRuleException invalidTransition(Status nextStatus) {
        return new BusinessRuleException("Payroll run cannot transition from " + this.status + " to " + nextStatus + ".",
                "PAYROLL_STATE_TRANSITION_INVALID", HttpStatus.CONFLICT);
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

    public String getRunNumber() {
        return runNumber;
    }

    public String getPeriodId() {
        return periodId;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public BigDecimal getTotalNet() {
        return totalNet;
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
        DRAFT, CALCULATED, REVIEWED, APPROVED, POSTED, PAID, CANCELLED
    }
}
