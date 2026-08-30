package com.bemo.hr.reporting.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_day_anomalies")
public class DayAnomaly {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "report_id", nullable = false)
    private String reportId;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Column(name = "category_id", nullable = false)
    private String categoryId;
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    @Column(length = 150)
    private String location;
    @Column(name = "affected_count", nullable = false)
    private int affectedCount;
    @Column(name = "total_employee_count", nullable = false)
    private int totalEmployeeCount;
    @Column(name = "absence_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal absencePercentage;
    @Column(name = "threshold_percentage", nullable = false)
    private int thresholdPercentage;
    @Column(name = "affected_expected_minutes", nullable = false)
    private int affectedExpectedMinutes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayAnomalyStatus status;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DayAnomalyDecision decision;
    @Column(length = 500)
    private String reason;
    @Column(name = "operation_id", length = 80)
    private String operationId;
    @Column(name = "decided_by", length = 160)
    private String decidedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "reversed_by", length = 160)
    private String reversedBy;
    @Column(name = "reversed_at")
    private Instant reversedAt;
    @Column(name = "reopened_by", length = 160)
    private String reopenedBy;
    @Column(name = "reopened_at")
    private Instant reopenedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DayAnomaly() {
    }

    public DayAnomaly(String reportId, LocalDate workDate, String categoryId, String categoryName,
                      String location, int affectedCount, int totalEmployeeCount,
                      BigDecimal absencePercentage, int thresholdPercentage, int affectedExpectedMinutes) {
        this.id = UUID.randomUUID().toString();
        this.reportId = reportId;
        this.workDate = workDate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.location = location;
        this.affectedCount = affectedCount;
        this.totalEmployeeCount = totalEmployeeCount;
        this.absencePercentage = absencePercentage;
        this.thresholdPercentage = thresholdPercentage;
        this.affectedExpectedMinutes = affectedExpectedMinutes;
        this.status = DayAnomalyStatus.OPEN;
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

    public void decide(DayAnomalyDecision decision, String reason, String operationId, String actor) {
        if (status != DayAnomalyStatus.OPEN)
            throw new BusinessRuleException("Anomaly status is not open for decision.", "ANOM_NOT_OPEN_FOR_DECISION", HttpStatus.CONFLICT);
        this.decision = decision;
        this.reason = reason;
        this.operationId = operationId;
        this.decidedBy = actor;
        this.decidedAt = Instant.now();
        this.status = decision == DayAnomalyDecision.DEFER ? DayAnomalyStatus.DEFERRED : DayAnomalyStatus.RESOLVED;
    }

    public void reverse(String actor) {
        if (status != DayAnomalyStatus.RESOLVED)
            throw new BusinessRuleException("Only resolved anomalies can be reversed.", "ANOM_REVERSE_RESOLVED_ONLY", HttpStatus.CONFLICT);
        status = DayAnomalyStatus.REVERSED;
        reversedBy = actor;
        reversedAt = Instant.now();
    }

    public void reopen(String actor) {
        if (status != DayAnomalyStatus.DEFERRED && status != DayAnomalyStatus.REVERSED) {
            throw new BusinessRuleException("Reverse the resolved decision first, or reopen the deferred/reversed status.", "ANOM_REOPEN_BEFORE_DECISION", HttpStatus.CONFLICT);
        }
        status = DayAnomalyStatus.OPEN;
        reopenedBy = actor;
        reopenedAt = Instant.now();
        operationId = null;
    }

    public boolean isReplay(String operationId) {
        return this.operationId != null && this.operationId.equals(operationId);
    }

    public String getId() {
        return id;
    }

    public String getReportId() {
        return reportId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getLocation() {
        return location;
    }

    public int getAffectedCount() {
        return affectedCount;
    }

    public int getTotalEmployeeCount() {
        return totalEmployeeCount;
    }

    public BigDecimal getAbsencePercentage() {
        return absencePercentage;
    }

    public int getThresholdPercentage() {
        return thresholdPercentage;
    }

    public int getAffectedExpectedMinutes() {
        return affectedExpectedMinutes;
    }

    public DayAnomalyStatus getStatus() {
        return status;
    }

    public DayAnomalyDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getReversedBy() {
        return reversedBy;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }

    public String getReopenedBy() {
        return reopenedBy;
    }

    public Instant getReopenedAt() {
        return reopenedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
