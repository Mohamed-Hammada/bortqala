package com.bemo.hr.payroll.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_retro_adjustments")
public class PayrollRetroAdjustment {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;
    @Column(name = "payroll_period_id", nullable = false, length = 36)
    private String payrollPeriodId;
    @Column(name = "adjustment_type", nullable = false, length = 50)
    private String adjustmentType;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(length = 255)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PayrollRetroAdjustment() {
    }

    public PayrollRetroAdjustment(String employeeId, String payrollPeriodId, String adjustmentType, BigDecimal amount, String reason) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.payrollPeriodId = payrollPeriodId;
        this.adjustmentType = adjustmentType;
        this.amount = amount;
        this.reason = reason;
        this.status = Status.PENDING;
    }

    public void approve() {
        this.status = Status.APPROVED;
    }

    public void process() {
        this.status = Status.PROCESSED;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public String getPayrollPeriodId() {
        return payrollPeriodId;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
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
        PENDING, APPROVED, PROCESSED
    }
}
