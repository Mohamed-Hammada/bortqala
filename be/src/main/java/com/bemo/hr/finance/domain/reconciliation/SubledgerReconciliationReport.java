package com.bemo.hr.finance.domain.reconciliation;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subledger_reconciliation_reports")
public class SubledgerReconciliationReport {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;
    @Enumerated(EnumType.STRING)
    @Column(name = "subledger_type", nullable = false, length = 20)
    private SubledgerType subledgerType;
    @Column(name = "gl_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal glBalance;
    @Column(name = "subledger_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal subledgerBalance;
    @Column(name = "variance_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceAmount;
    @Column(name = "reconciled_at", nullable = false)
    private long reconciledAt;
    @Column(name = "as_of_date", nullable = false)
    private java.time.LocalDate asOfDate;
    @Column(name = "difference_details", nullable = false, length = 8000)
    private String differenceDetails;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected SubledgerReconciliationReport() {
    }

    public SubledgerReconciliationReport(String periodId, SubledgerType subledgerType, BigDecimal glBalance, BigDecimal subledgerBalance) {
        this(periodId, subledgerType, glBalance, subledgerBalance, java.time.LocalDate.now(), "[]");
    }

    public SubledgerReconciliationReport(String periodId, SubledgerType subledgerType, BigDecimal glBalance, BigDecimal subledgerBalance,
                                         java.time.LocalDate asOfDate, String differenceDetails) {
        this.id = UUID.randomUUID().toString();
        this.periodId = periodId;
        this.subledgerType = subledgerType;
        this.glBalance = glBalance;
        this.subledgerBalance = subledgerBalance;
        this.varianceAmount = glBalance.subtract(subledgerBalance);
        this.reconciledAt = System.currentTimeMillis();
        this.asOfDate = asOfDate;
        this.differenceDetails = differenceDetails;
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

    public SubledgerType getSubledgerType() {
        return subledgerType;
    }

    public BigDecimal getGlBalance() {
        return glBalance;
    }

    public BigDecimal getSubledgerBalance() {
        return subledgerBalance;
    }

    public BigDecimal getVarianceAmount() {
        return varianceAmount;
    }

    public long getReconciledAt() {
        return reconciledAt;
    }

    public java.time.LocalDate getAsOfDate() {
        return asOfDate;
    }

    public String getDifferenceDetails() {
        return differenceDetails;
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

    public enum SubledgerType {
        AR, AP, INVENTORY, TREASURY, PAYROLL
    }
}
