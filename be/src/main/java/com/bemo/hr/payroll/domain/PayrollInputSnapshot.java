package com.bemo.hr.payroll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_input_snapshots")
public class PayrollInputSnapshot {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;

    @Column(name = "worked_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal workedHours;

    @Column(name = "overtime_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtimeHours;

    @Column(name = "absence_days", nullable = false)
    private int absenceDays;

    @Column(name = "deduction_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal deductionAmount;

    @Column(name = "allowance_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal allowanceAmount;

    @Column(name = "gross_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "net_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPay;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at", nullable = false)
    private long lockedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PayrollInputSnapshot() {}

    public PayrollInputSnapshot(String employeeId, String periodId, BigDecimal workedHours, BigDecimal overtimeHours,
                                int absenceDays, BigDecimal deductionAmount, BigDecimal allowanceAmount,
                                BigDecimal grossPay, BigDecimal netPay, String lockedBy) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.periodId = periodId;
        this.workedHours = workedHours;
        this.overtimeHours = overtimeHours;
        this.absenceDays = absenceDays;
        this.deductionAmount = deductionAmount;
        this.allowanceAmount = allowanceAmount;
        this.grossPay = grossPay;
        this.netPay = netPay;
        this.lockedBy = lockedBy;
        this.lockedAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getEmployeeId() { return employeeId; }
    public String getPeriodId() { return periodId; }
    public BigDecimal getWorkedHours() { return workedHours; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public int getAbsenceDays() { return absenceDays; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public BigDecimal getAllowanceAmount() { return allowanceAmount; }
    public BigDecimal getGrossPay() { return grossPay; }
    public BigDecimal getNetPay() { return netPay; }
    public String getLockedBy() { return lockedBy; }
    public long getLockedAt() { return lockedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
