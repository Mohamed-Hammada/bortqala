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
import java.time.LocalDate;
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
    @Column(name = "scope_key", nullable = false, length = 36) private String scopeKey;
    @Column(name = "deduction_mode", nullable = false, length = 20) private String deductionMode;
    @Column(name = "deduction_frequency", nullable = false, length = 30) private String deductionFrequency;
    @Column(name = "max_deduction_percent", nullable = false, precision = 5, scale = 2) private BigDecimal maxDeductionPercent;
    @Column(name = "default_installments", nullable = false) private int defaultInstallments;
    @Column(name = "deferral_periods", nullable = false) private int deferralPeriods;
    @Column(nullable = false) private int version;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkforceAdvancePolicy() { }

    public WorkforceAdvancePolicy(String scopeType, String scopeId, String deductionMode,
                                  String deductionFrequency, BigDecimal maxDeductionPercent,
                                  int defaultInstallments, int deferralPeriods, boolean active,
                                  int version, LocalDate effectiveFrom, LocalDate effectiveTo) {
        id = UUID.randomUUID().toString();
        update(scopeType, scopeId, deductionMode, deductionFrequency, maxDeductionPercent,
                defaultInstallments, deferralPeriods, active, effectiveFrom, effectiveTo);
        this.version = Math.max(1, version);
    }

    public void update(String scopeType, String scopeId, String deductionMode,
                       String deductionFrequency, BigDecimal maxDeductionPercent,
                       int defaultInstallments, int deferralPeriods, boolean active,
                       LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.scopeType = scopeType.strip().toUpperCase(Locale.ROOT);
        this.scopeId = "GLOBAL".equals(this.scopeType) ? null : scopeId;
        this.scopeKey = this.scopeId == null ? "GLOBAL" : this.scopeId;
        this.deductionMode = deductionMode == null ? "AUTO" : deductionMode.strip().toUpperCase(Locale.ROOT);
        this.deductionFrequency = deductionFrequency == null ? "HALF_MONTH" : deductionFrequency.strip().toUpperCase(Locale.ROOT);
        this.maxDeductionPercent = maxDeductionPercent == null ? new BigDecimal("50") : maxDeductionPercent;
        this.defaultInstallments = Math.max(1, defaultInstallments);
        this.deferralPeriods = Math.max(0, deferralPeriods);
        this.active = active;
        this.effectiveFrom = effectiveFrom == null ? LocalDate.now() : effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public boolean isEffectiveOn(LocalDate date) {
        return active && !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    public void closeBefore(LocalDate nextEffectiveFrom) {
        LocalDate closingDate = nextEffectiveFrom.minusDays(1);
        if (!closingDate.isBefore(effectiveFrom)) this.effectiveTo = closingDate;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
