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
import java.util.UUID;

@Entity
@Table(name = "payroll_payment_batches")
public class PayrollPaymentBatch {

    public enum Status {
        DRAFT, SUBMITTED, PROCESSED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "payroll_period_id", nullable = false, length = 36)
    private String payrollPeriodId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "employee_count", nullable = false)
    private int employeeCount;

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

    protected PayrollPaymentBatch() {}

    public PayrollPaymentBatch(String payrollPeriodId, BigDecimal totalAmount, int employeeCount) {
        this.id = UUID.randomUUID().toString();
        this.batchNumber = "BATCH-" + System.currentTimeMillis();
        this.payrollPeriodId = payrollPeriodId;
        this.totalAmount = totalAmount;
        this.employeeCount = employeeCount;
        this.status = Status.DRAFT;
    }

    public void process() {
        this.status = Status.PROCESSED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getBatchNumber() { return batchNumber; }
    public String getPayrollPeriodId() { return payrollPeriodId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getEmployeeCount() { return employeeCount; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
