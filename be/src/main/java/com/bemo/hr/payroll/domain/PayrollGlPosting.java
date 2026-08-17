package com.bemo.hr.payroll.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_gl_postings")
public class PayrollGlPosting {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "payroll_period_id", nullable = false, length = 36)
    private String payrollPeriodId;
    @Column(name = "journal_id", nullable = false, length = 36)
    private String journalId;
    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;
    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;
    @Column(name = "posted_at", nullable = false)
    private long postedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.POSTED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PayrollGlPosting() {
    }

    public PayrollGlPosting(String payrollPeriodId, String journalId, BigDecimal grossAmount, BigDecimal netAmount) {
        this.id = UUID.randomUUID().toString();
        this.payrollPeriodId = payrollPeriodId;
        this.journalId = journalId;
        this.grossAmount = grossAmount;
        this.netAmount = netAmount;
        this.postedAt = System.currentTimeMillis();
        this.status = Status.POSTED;
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

    public String getPayrollPeriodId() {
        return payrollPeriodId;
    }

    public String getJournalId() {
        return journalId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public long getPostedAt() {
        return postedAt;
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
        POSTED
    }
}
