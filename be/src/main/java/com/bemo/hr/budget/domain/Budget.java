package com.bemo.hr.budget.domain;

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
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private BudgetPeriodType periodType;

    @Column(name = "period_month")
    private Integer periodMonth;

    @Column(name = "department_id", nullable = false, length = 36)
    private String departmentId;

    @Column(name = "planned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "EGP";

    @Column(nullable = false)
    private boolean blocking = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Budget() {}

    public Budget(int fiscalYear, BudgetPeriodType periodType, Integer periodMonth,
                  String departmentId, BigDecimal plannedAmount, String currencyCode,
                  boolean blocking, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(fiscalYear, periodType, periodMonth, departmentId, plannedAmount, currencyCode, blocking, active);
    }

    public void update(int fiscalYear, BudgetPeriodType periodType, Integer periodMonth,
                       String departmentId, BigDecimal plannedAmount, String currencyCode,
                       boolean blocking, boolean active) {
        if (plannedAmount == null || plannedAmount.signum() < 0) {
            throw new IllegalArgumentException("Planned amount must be non-negative.");
        }
        this.fiscalYear = fiscalYear;
        this.periodType = periodType == null ? BudgetPeriodType.ANNUAL : periodType;
        this.periodMonth = this.periodType == BudgetPeriodType.MONTHLY ? periodMonth : null;
        this.departmentId = departmentId;
        this.plannedAmount = plannedAmount;
        this.currencyCode = currencyCode == null || currencyCode.isBlank() ? "EGP" : currencyCode.strip().toUpperCase();
        this.blocking = blocking;
        this.active = active;
    }

    public void activate() { this.active = true; }

    public void deactivate() { this.active = false; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public int getFiscalYear() { return fiscalYear; }
    public BudgetPeriodType getPeriodType() { return periodType; }
    public Integer getPeriodMonth() { return periodMonth; }
    public String getDepartmentId() { return departmentId; }
    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isBlocking() { return blocking; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
