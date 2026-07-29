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
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "workforce_advance_policies")
@Getter
public class WorkforceAdvancePolicy {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "scope_type", nullable = false, length = 20) private String scopeType;
    @Column(name = "scope_id", length = 36) private String scopeId;
    @Column(name = "deduction_mode", nullable = false, length = 20) private String deductionMode;
    @Column(name = "deduction_frequency", nullable = false, length = 30) private String deductionFrequency;
    @Column(name = "max_deduction_percent", nullable = false, precision = 5, scale = 2) private BigDecimal maxDeductionPercent;
    @Column(name = "default_installments", nullable = false) private int defaultInstallments;
    @Column(name = "deferral_periods", nullable = false) private int deferralPeriods;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkforceAdvancePolicy() { }

    public WorkforceAdvancePolicy(String scopeType, String scopeId, String deductionMode,
                                  String deductionFrequency, BigDecimal maxDeductionPercent,
                                  int defaultInstallments, int deferralPeriods, boolean active) {
        id = UUID.randomUUID().toString();
        update(scopeType, scopeId, deductionMode, deductionFrequency, maxDeductionPercent,
                defaultInstallments, deferralPeriods, active);
    }

    public void update(String scopeType, String scopeId, String deductionMode,
                       String deductionFrequency, BigDecimal maxDeductionPercent,
                       int defaultInstallments, int deferralPeriods, boolean active) {
        this.scopeType = scopeType.strip().toUpperCase(Locale.ROOT);
        this.scopeId = "GLOBAL".equals(this.scopeType) ? null : scopeId;
        this.deductionMode = deductionMode == null ? "AUTO" : deductionMode.strip().toUpperCase(Locale.ROOT);
        this.deductionFrequency = deductionFrequency == null ? "HALF_MONTH" : deductionFrequency.strip().toUpperCase(Locale.ROOT);
        this.maxDeductionPercent = maxDeductionPercent == null ? new BigDecimal("50") : maxDeductionPercent;
        this.defaultInstallments = Math.max(1, defaultInstallments);
        this.deferralPeriods = Math.max(0, deferralPeriods);
        this.active = active;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
