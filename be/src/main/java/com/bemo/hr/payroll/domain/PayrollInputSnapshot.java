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
import java.time.LocalDate;
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

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "base_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "worked_minutes", nullable = false)
    private long workedMinutes;

    @Column(name = "overtime_minutes", nullable = false)
    private long overtimeMinutes;

    @Column(name = "late_minutes", nullable = false)
    private long lateMinutes;

    @Column(name = "payroll_policy_id", nullable = false, length = 36)
    private String payrollPolicyId;

    @Column(name = "payroll_policy_version", nullable = false)
    private long payrollPolicyVersion;

    @Column(name = "working_hour_divisor", nullable = false, precision = 10, scale = 2)
    private BigDecimal workingHourDivisor;

    @Column(name = "overtime_multiplier", nullable = false, precision = 10, scale = 4)
    private BigDecimal overtimeMultiplier;

    @Column(name = "advance_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal advanceBalance;

    @Column(name = "advance_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal advanceDeduction;

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

    public PayrollInputSnapshot(String employeeId, String periodId, LocalDate periodStart, LocalDate periodEnd,
                                BigDecimal baseSalary, long workedMinutes, long overtimeMinutes, long lateMinutes,
                                int absenceDays, String payrollPolicyId, long payrollPolicyVersion,
                                BigDecimal workingHourDivisor, BigDecimal overtimeMultiplier,
                                BigDecimal deductionAmount, BigDecimal allowanceAmount, BigDecimal advanceBalance,
                                BigDecimal advanceDeduction, BigDecimal grossPay, BigDecimal netPay, String lockedBy) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.periodId = periodId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.baseSalary = baseSalary;
        this.workedMinutes = workedMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.lateMinutes = lateMinutes;
        this.workedHours = BigDecimal.valueOf(workedMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        this.overtimeHours = BigDecimal.valueOf(overtimeMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        this.absenceDays = absenceDays;
        this.payrollPolicyId = payrollPolicyId;
        this.payrollPolicyVersion = payrollPolicyVersion;
        this.workingHourDivisor = workingHourDivisor;
        this.overtimeMultiplier = overtimeMultiplier;
        this.deductionAmount = deductionAmount;
        this.allowanceAmount = allowanceAmount;
        this.advanceBalance = advanceBalance;
        this.advanceDeduction = advanceDeduction;
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
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getBaseSalary() { return baseSalary; }
    public long getWorkedMinutes() { return workedMinutes; }
    public long getOvertimeMinutes() { return overtimeMinutes; }
    public long getLateMinutes() { return lateMinutes; }
    public String getPayrollPolicyId() { return payrollPolicyId; }
    public long getPayrollPolicyVersion() { return payrollPolicyVersion; }
    public BigDecimal getWorkingHourDivisor() { return workingHourDivisor; }
    public BigDecimal getOvertimeMultiplier() { return overtimeMultiplier; }
    public BigDecimal getAdvanceBalance() { return advanceBalance; }
    public BigDecimal getAdvanceDeduction() { return advanceDeduction; }
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
