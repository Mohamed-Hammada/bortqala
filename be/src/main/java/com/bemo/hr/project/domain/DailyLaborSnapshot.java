package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "daily_labor_snapshots")
public class DailyLaborSnapshot {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "daily_report_id", length = 36, nullable = false)
    private String dailyReportId;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;

    @Column(name = "trade_category", length = 64, nullable = false)
    private String tradeCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 32, nullable = false)
    private LaborSourceType sourceType;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(name = "headcount", nullable = false)
    private int headcount;

    @Column(name = "hours_worked", precision = 6, scale = 2, nullable = false)
    private BigDecimal hoursWorked;

    @Column(name = "activity_description", columnDefinition = "TEXT")
    private String activityDescription;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyLaborSnapshot() {
    }

    public DailyLaborSnapshot(String dailyReportId, String wbsNodeId, String costCodeId,
                              String tradeCategory, LaborSourceType sourceType, String partyId,
                              int headcount, BigDecimal hoursWorked, String activityDescription) {
        this.id = UUID.randomUUID().toString();
        this.dailyReportId = Objects.requireNonNull(dailyReportId, "dailyReportId must not be null");
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.tradeCategory = Objects.requireNonNull(tradeCategory, "tradeCategory must not be null");
        this.sourceType = sourceType != null ? sourceType : LaborSourceType.DIRECT_EMPLOYEE;
        this.partyId = partyId;
        this.headcount = Math.max(1, headcount);
        this.hoursWorked = hoursWorked != null ? hoursWorked : BigDecimal.valueOf(8.0);
        this.activityDescription = activityDescription;
        this.createdAt = Instant.now();
    }

    public BigDecimal getTotalManHours() {
        return hoursWorked.multiply(BigDecimal.valueOf(headcount));
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

    public String getCostCodeId() {
        return costCodeId;
    }

    public String getTradeCategory() {
        return tradeCategory;
    }

    public LaborSourceType getSourceType() {
        return sourceType;
    }

    public String getPartyId() {
        return partyId;
    }

    public int getHeadcount() {
        return headcount;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
