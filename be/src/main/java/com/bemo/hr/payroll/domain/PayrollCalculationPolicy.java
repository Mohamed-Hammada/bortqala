package com.bemo.hr.payroll.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_calculation_policies")
public class PayrollCalculationPolicy {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Column(name = "working_hour_divisor", nullable = false, precision = 10, scale = 2)
    private BigDecimal workingHourDivisor;
    @Column(name = "overtime_multiplier", nullable = false, precision = 10, scale = 4)
    private BigDecimal overtimeMultiplier;
    @Column(nullable = false)
    private boolean active;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PayrollCalculationPolicy() {
    }

    public PayrollCalculationPolicy(String name, LocalDate effectiveFrom, LocalDate effectiveTo,
                                    BigDecimal workingHourDivisor, BigDecimal overtimeMultiplier) {
        validate(effectiveFrom, effectiveTo, workingHourDivisor, overtimeMultiplier);
        this.id = UUID.randomUUID().toString();
        this.name = name == null || name.isBlank() ? "Standard payroll policy" : name.strip();
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.workingHourDivisor = workingHourDivisor;
        this.overtimeMultiplier = overtimeMultiplier;
        this.active = true;
    }

    private static void validate(LocalDate from, LocalDate to, BigDecimal divisor, BigDecimal multiplier) {
        if (from == null || (to != null && to.isBefore(from))) {
            throw new BusinessRuleException("Payroll policy effective dates are invalid.", "PAYROLL_POLICY_DATES_INVALID", HttpStatus.CONFLICT);
        }
        if (divisor == null || divisor.signum() <= 0) {
            throw new BusinessRuleException("Payroll working-hour divisor must be positive.", "PAYROLL_POLICY_DIVISOR_INVALID", HttpStatus.CONFLICT);
        }
        if (multiplier == null || multiplier.signum() < 0) {
            throw new BusinessRuleException("Payroll overtime multiplier cannot be negative.", "PAYROLL_POLICY_MULTIPLIER_INVALID", HttpStatus.CONFLICT);
        }
    }

    public boolean appliesOn(LocalDate date) {
        return active && !effectiveFrom.isAfter(date) && (effectiveTo == null || !effectiveTo.isBefore(date));
    }

    public void closeBefore(LocalDate nextEffectiveFrom) {
        if (nextEffectiveFrom == null || !nextEffectiveFrom.isAfter(effectiveFrom)) {
            throw new BusinessRuleException("A replacement payroll policy must start after the current policy.",
                    "PAYROLL_POLICY_DATES_INVALID", HttpStatus.CONFLICT);
        }
        this.effectiveTo = nextEffectiveFrom.minusDays(1);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public BigDecimal getWorkingHourDivisor() {
        return workingHourDivisor;
    }

    public BigDecimal getOvertimeMultiplier() {
        return overtimeMultiplier;
    }

    public long getVersion() {
        return version;
    }
}
