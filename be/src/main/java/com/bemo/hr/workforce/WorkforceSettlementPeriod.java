package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_settlement_periods")
@Getter
public class WorkforceSettlementPeriod {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "period_code", nullable = false, length = 50) private String periodCode;
    @Column(name = "start_date", nullable = false, length = 10) private String startDate;
    @Column(name = "end_date", nullable = false, length = 10) private String endDate;
    @Column(name = "cycle_type", nullable = false, length = 30) private String cycleType;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "calculation_version", nullable = false) private int calculationVersion;
    @Column(name = "last_calculated_at") private Instant lastCalculatedAt;
    @Column(name = "last_calculated_by", length = 160) private String lastCalculatedBy;
    @Column(name = "last_calculation_failed_at") private Instant lastCalculationFailedAt;
    @Column(name = "last_calculation_error", length = 1000) private String lastCalculationError;
    @Column(name = "input_fingerprint", length = 64) private String inputFingerprint;
    @Column(name = "result_record_count", nullable = false) private int resultRecordCount;
    @Column(name = "result_gross_amount", nullable = false, precision = 15, scale = 2) private BigDecimal resultGrossAmount;
    @Column(name = "result_deductions", nullable = false, precision = 15, scale = 2) private BigDecimal resultDeductions;
    @Column(name = "result_advances", nullable = false, precision = 15, scale = 2) private BigDecimal resultAdvances;
    @Column(name = "result_net_amount", nullable = false, precision = 15, scale = 2) private BigDecimal resultNetAmount;
    @Column(name = "result_warning_count", nullable = false) private int resultWarningCount;
    @Column(name = "result_error_count", nullable = false) private int resultErrorCount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkforceSettlementPeriod() { }

    public WorkforceSettlementPeriod(String periodCode, String startDate, String endDate, String cycleType, String status) {
        this.id = UUID.randomUUID().toString();
        this.periodCode = periodCode != null ? periodCode.strip().toUpperCase() : "SETTL-" + UUID.randomUUID().toString().substring(0, 6);
        this.startDate = startDate;
        this.endDate = endDate;
        this.cycleType = cycleType != null ? cycleType.strip().toUpperCase() : "HALF_MONTH";
        this.status = status != null ? status.strip().toUpperCase() : "DRAFT";
        this.resultGrossAmount = BigDecimal.ZERO;
        this.resultDeductions = BigDecimal.ZERO;
        this.resultAdvances = BigDecimal.ZERO;
        this.resultNetAmount = BigDecimal.ZERO;
    }

    public void setStatus(String status) {
        this.status = status.strip().toUpperCase();
    }

    public void markCalculated(String actor, String fingerprint, int recordCount,
                               BigDecimal grossAmount, BigDecimal deductions, BigDecimal advances,
                               BigDecimal netAmount, int warningCount, int errorCount) {
        this.calculationVersion++;
        this.status = "CALCULATED";
        this.lastCalculatedAt = Instant.now();
        this.lastCalculatedBy = actor;
        this.lastCalculationFailedAt = null;
        this.lastCalculationError = null;
        this.inputFingerprint = fingerprint;
        this.resultRecordCount = recordCount;
        this.resultGrossAmount = grossAmount;
        this.resultDeductions = deductions;
        this.resultAdvances = advances;
        this.resultNetAmount = netAmount;
        this.resultWarningCount = warningCount;
        this.resultErrorCount = errorCount;
    }

    public void markCalculationFailed(String message) {
        this.lastCalculationFailedAt = Instant.now();
        this.lastCalculationError = message == null ? "تعذر تنفيذ إعادة الاحتساب." : message.substring(0, Math.min(message.length(), 1000));
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
