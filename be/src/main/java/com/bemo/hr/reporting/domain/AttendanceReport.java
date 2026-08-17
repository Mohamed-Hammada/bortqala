package com.bemo.hr.reporting.domain;

import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class AttendanceReport {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_cycle", nullable = false, length = 20)
    private PayCycle payCycle;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;
    @Column(name = "configuration_version", nullable = false, length = 64)
    private String configurationVersion;
    @Column(name = "generation_hash", length = 64)
    private String generationHash;
    @Column(name = "unresolved_count", nullable = false)
    private int unresolvedCount;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "exported_at")
    private Instant exportedAt;
    @Version
    private long version;

    protected AttendanceReport() {
    }

    public AttendanceReport(LocalDate periodStart, LocalDate periodEnd, PayCycle payCycle,
                            String configurationVersion, String createdBy) {
        this(periodStart, periodEnd, payCycle, configurationVersion, null, createdBy);
    }

    public AttendanceReport(LocalDate periodStart, LocalDate periodEnd, PayCycle payCycle,
                            String configurationVersion, String generationHash, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.payCycle = payCycle;
        this.configurationVersion = configurationVersion;
        this.generationHash = generationHash;
        this.createdBy = createdBy;
        this.status = ReportStatus.DRAFT;
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

    public void startReview(int unresolvedCount) {
        this.unresolvedCount = unresolvedCount;
        this.status = ReportStatus.IN_REVIEW;
    }

    public void updateUnresolvedCount(int value) {
        this.unresolvedCount = value;
    }

    public void approve(String actor) {
        if (status != ReportStatus.IN_REVIEW || unresolvedCount != 0)
            throw new BusinessRuleException("Resolve every blocking item before approval.", "RPT_UNRESOLVED_BLOCKERS", HttpStatus.CONFLICT);
        status = ReportStatus.APPROVED;
        approvedBy = actor;
        approvedAt = Instant.now();
    }

    public void markExported() {
        if (status == ReportStatus.APPROVED || status == ReportStatus.EXPORTED) {
            status = ReportStatus.EXPORTED;
            exportedAt = Instant.now();
        }
    }

    public void reopen() {
        if (status != ReportStatus.APPROVED && status != ReportStatus.EXPORTED)
            throw new BusinessRuleException("Only approved or exported reports can be reopened.", "RPT_REOPEN_NOT_ALLOWED", HttpStatus.CONFLICT);
        status = ReportStatus.IN_REVIEW;
        approvedBy = null;
        approvedAt = null;
        exportedAt = null;
    }

    public String getId() {
        return id;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public PayCycle getPayCycle() {
        return payCycle;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getConfigurationVersion() {
        return configurationVersion;
    }

    public String getGenerationHash() {
        return generationHash;
    }

    public int getUnresolvedCount() {
        return unresolvedCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getExportedAt() {
        return exportedAt;
    }

    public long getVersion() {
        return version;
    }
}
