package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_advances")
@Getter
public class WorkforceAdvance {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "recipient_type", nullable = false, length = 30) private String recipientType;
    @Column(name = "worker_id", length = 36) private String workerId;
    @Column(name = "contractor_id", length = 36) private String contractorId;
    @Column(precision = 12, scale = 2, nullable = false) private BigDecimal amount;
    @Column(name = "term_type", nullable = false, length = 30) private String termType;
    @Column(name = "total_installments", nullable = false) private int totalInstallments;
    @Column(name = "installment_amount", precision = 12, scale = 2, nullable = false) private BigDecimal installmentAmount;
    @Column(name = "remaining_balance", precision = 12, scale = 2, nullable = false) private BigDecimal remainingBalance;
    @Column(name = "deduction_frequency", nullable = false, length = 30) private String deductionFrequency;
    @Column(name = "max_deduction_percent", precision = 5, scale = 2) private BigDecimal maxDeductionPercent;
    @Column(nullable = false, length = 30) private String status;
    @Column(length = 500) private String reason;
    @Column(name = "first_installment_date", length = 10) private String firstInstallmentDate;
    @Column(name = "deduction_mode", length = 20) private String deductionMode;
    @Column(name = "deferral_periods") private Integer deferralPeriods;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkforceAdvance() { }

    public WorkforceAdvance(String recipientType, String workerId, String contractorId,
                            BigDecimal amount, String termType, int totalInstallments,
                            BigDecimal installmentAmount, String deductionFrequency,
                            BigDecimal maxDeductionPercent, String reason,
                            String firstInstallmentDate, String deductionMode,
                            Integer deferralPeriods) {
        this.id = UUID.randomUUID().toString();
        this.recipientType = recipientType != null ? recipientType.strip().toUpperCase() : "WORKER";
        this.workerId = workerId;
        this.contractorId = contractorId;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.termType = termType != null ? termType.strip().toUpperCase() : "SHORT_TERM";
        this.totalInstallments = Math.max(1, totalInstallments);
        this.installmentAmount = installmentAmount != null ? installmentAmount : this.amount;
        this.remainingBalance = this.amount;
        this.deductionFrequency = deductionFrequency != null ? deductionFrequency.strip().toUpperCase() : "HALF_MONTH";
        this.maxDeductionPercent = maxDeductionPercent != null ? maxDeductionPercent : new BigDecimal("50.0");
        this.status = "ACTIVE";
        this.reason = reason;
        this.firstInstallmentDate = firstInstallmentDate;
        this.deductionMode = deductionMode != null ? deductionMode : "AUTO";
        this.deferralPeriods = deferralPeriods != null ? deferralPeriods : 0;
    }

    public void deduct(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) return;
        this.remainingBalance = this.remainingBalance.subtract(value);
        if (this.remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.remainingBalance = BigDecimal.ZERO;
            this.status = "PAID_OFF";
        }
    }

    public void pause() { this.status = "PAUSED"; }
    public void resume() { this.status = "ACTIVE"; }
    public void repay(BigDecimal value) { deduct(value); }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
