package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "project_daily_reports")
public class ProjectDailyReport {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "report_number", length = 64, nullable = false)
    private String reportNumber;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 32, nullable = false)
    private ReportShift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_condition", length = 32)
    private WeatherCondition weatherCondition;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private BigDecimal temperatureCelsius;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private DailyReportStatus status;

    @Column(name = "site_engineer_user_id", length = 64)
    private String siteEngineerUserId;

    @Column(name = "approver_user_id", length = 64)
    private String approverUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "general_notes", columnDefinition = "TEXT")
    private String generalNotes;

    @Column(name = "blockers_and_issues", columnDefinition = "TEXT")
    private String blockersAndIssues;

    @Column(name = "safety_observations", columnDefinition = "TEXT")
    private String safetyObservations;

    @Column(name = "total_workforce_count", nullable = false)
    private int totalWorkforceCount;

    @Column(name = "total_equipment_count", nullable = false)
    private int totalEquipmentCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectDailyReport() {
    }

    public ProjectDailyReport(String projectId, String reportNumber, LocalDate reportDate, ReportShift shift,
                              WeatherCondition weatherCondition, BigDecimal temperatureCelsius,
                              String siteEngineerUserId, String generalNotes, String blockersAndIssues,
                              String safetyObservations) {
        this.id = UUID.randomUUID().toString();
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.reportNumber = Objects.requireNonNull(reportNumber, "reportNumber must not be null");
        this.reportDate = Objects.requireNonNull(reportDate, "reportDate must not be null");
        this.shift = shift != null ? shift : ReportShift.DAY;
        this.weatherCondition = weatherCondition;
        this.temperatureCelsius = temperatureCelsius;
        this.status = DailyReportStatus.DRAFT;
        this.siteEngineerUserId = siteEngineerUserId;
        this.generalNotes = generalNotes;
        this.blockersAndIssues = blockersAndIssues;
        this.safetyObservations = safetyObservations;
        this.totalWorkforceCount = 0;
        this.totalEquipmentCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateDraft(ReportShift shift, WeatherCondition weatherCondition,
                            BigDecimal temperatureCelsius, String generalNotes,
                            String blockersAndIssues, String safetyObservations) {
        if (this.status == DailyReportStatus.APPROVED) {
            throw new IllegalStateException("Approved daily report cannot be updated without reopening");
        }
        if (shift != null) {
            this.shift = shift;
        }
        this.weatherCondition = weatherCondition;
        this.temperatureCelsius = temperatureCelsius;
        this.generalNotes = generalNotes;
        this.blockersAndIssues = blockersAndIssues;
        this.safetyObservations = safetyObservations;
        this.updatedAt = Instant.now();
    }

    public void updateTotals(int workforceCount, int equipmentCount) {
        this.totalWorkforceCount = Math.max(0, workforceCount);
        this.totalEquipmentCount = Math.max(0, equipmentCount);
        this.updatedAt = Instant.now();
    }

    public void submit(String userId) {
        if (this.status == DailyReportStatus.APPROVED) {
            throw new IllegalStateException("Daily report is already approved");
        }
        this.status = DailyReportStatus.SUBMITTED;
        if (userId != null && !userId.isBlank()) {
            this.siteEngineerUserId = userId;
        }
        this.updatedAt = Instant.now();
    }

    public void approve(String approverId) {
        this.status = DailyReportStatus.APPROVED;
        this.approverUserId = approverId;
        this.approvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reopen(String userId) {
        this.status = DailyReportStatus.REOPENED;
        this.reopenedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getReportNumber() {
        return reportNumber;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public ReportShift getShift() {
        return shift;
    }

    public WeatherCondition getWeatherCondition() {
        return weatherCondition;
    }

    public BigDecimal getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public DailyReportStatus getStatus() {
        return status;
    }

    public String getSiteEngineerUserId() {
        return siteEngineerUserId;
    }

    public String getApproverUserId() {
        return approverUserId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getReopenedAt() {
        return reopenedAt;
    }

    public String getGeneralNotes() {
        return generalNotes;
    }

    public String getBlockersAndIssues() {
        return blockersAndIssues;
    }

    public String getSafetyObservations() {
        return safetyObservations;
    }

    public int getTotalWorkforceCount() {
        return totalWorkforceCount;
    }

    public int getTotalEquipmentCount() {
        return totalEquipmentCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
