package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "daily_work_progress_lines")
public class DailyWorkProgressLine {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "daily_report_id", length = 36, nullable = false)
    private String dailyReportId;

    @Column(name = "wbs_node_id", length = 36, nullable = false)
    private String wbsNodeId;

    @Column(name = "wbs_code", length = 64, nullable = false)
    private String wbsCode;

    @Column(name = "wbs_name", length = 255, nullable = false)
    private String wbsName;

    @Column(name = "unit_of_measure", length = 32)
    private String unitOfMeasure;

    @Column(name = "previous_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal previousQuantity;

    @Column(name = "today_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal todayQuantity;

    @Column(name = "cumulative_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal cumulativeQuantity;

    @Column(name = "percent_complete", precision = 7, scale = 2, nullable = false)
    private BigDecimal percentComplete;

    @Column(name = "location_notes", length = 255)
    private String locationNotes;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyWorkProgressLine() {
    }

    public DailyWorkProgressLine(String dailyReportId, String wbsNodeId, String wbsCode, String wbsName,
                                 String unitOfMeasure, BigDecimal previousQuantity, BigDecimal todayQuantity,
                                 BigDecimal plannedQuantity, String locationNotes, String remarks) {
        this.id = UUID.randomUUID().toString();
        this.dailyReportId = Objects.requireNonNull(dailyReportId, "dailyReportId must not be null");
        this.wbsNodeId = Objects.requireNonNull(wbsNodeId, "wbsNodeId must not be null");
        this.wbsCode = Objects.requireNonNull(wbsCode, "wbsCode must not be null");
        this.wbsName = Objects.requireNonNull(wbsName, "wbsName must not be null");
        this.unitOfMeasure = unitOfMeasure;
        this.previousQuantity = previousQuantity != null ? previousQuantity : BigDecimal.ZERO;
        this.todayQuantity = todayQuantity != null ? todayQuantity : BigDecimal.ZERO;
        this.cumulativeQuantity = this.previousQuantity.add(this.todayQuantity);
        this.locationNotes = locationNotes;
        this.remarks = remarks;
        this.percentComplete = calculatePercent(this.cumulativeQuantity, plannedQuantity);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(BigDecimal previousQuantity, BigDecimal todayQuantity, BigDecimal plannedQuantity,
                       String locationNotes, String remarks) {
        this.previousQuantity = previousQuantity != null ? previousQuantity : BigDecimal.ZERO;
        this.todayQuantity = todayQuantity != null ? todayQuantity : BigDecimal.ZERO;
        this.cumulativeQuantity = this.previousQuantity.add(this.todayQuantity);
        this.locationNotes = locationNotes;
        this.remarks = remarks;
        this.percentComplete = calculatePercent(this.cumulativeQuantity, plannedQuantity);
        this.updatedAt = Instant.now();
    }

    private static BigDecimal calculatePercent(BigDecimal cumulative, BigDecimal planned) {
        if (planned == null || planned.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return cumulative.multiply(BigDecimal.valueOf(100))
                .divide(planned, 2, java.math.RoundingMode.HALF_UP);
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDailyReportId() {
        return dailyReportId;
    }

    public String getWbsNodeId() {
        return wbsNodeId;
    }

    public String getWbsCode() {
        return wbsCode;
    }

    public String getWbsName() {
        return wbsName;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public BigDecimal getTodayQuantity() {
        return todayQuantity;
    }

    public BigDecimal getCumulativeQuantity() {
        return cumulativeQuantity;
    }

    public BigDecimal getPercentComplete() {
        return percentComplete;
    }

    public String getLocationNotes() {
        return locationNotes;
    }

    public String getRemarks() {
        return remarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
