package com.bemo.hr.analytics.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "executive_cockpit_targets")
public class ExecutiveCockpitTarget {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "period_key", length = 20, nullable = false)
    private String periodKey;

    @Column(name = "target_revenue", precision = 15, scale = 2)
    private BigDecimal targetRevenue;

    @Column(name = "target_gross_margin_percent", precision = 5, scale = 2)
    private BigDecimal targetGrossMarginPercent;

    @Column(name = "target_max_opex", precision = 15, scale = 2)
    private BigDecimal targetMaxOpex;

    @Column(name = "target_min_liquidity", precision = 15, scale = 2)
    private BigDecimal targetMinLiquidity;

    @Column(name = "target_max_overdue_ar", precision = 15, scale = 2)
    private BigDecimal targetMaxOverdueAr;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExecutiveCockpitTarget() {
    }

    public ExecutiveCockpitTarget(
            String periodKey,
            BigDecimal targetRevenue,
            BigDecimal targetGrossMarginPercent,
            BigDecimal targetMaxOpex,
            BigDecimal targetMinLiquidity,
            BigDecimal targetMaxOverdueAr,
            String notes
    ) {
        this.id = UUID.randomUUID().toString();
        this.periodKey = Objects.requireNonNull(periodKey, "periodKey cannot be null").trim();
        this.targetRevenue = targetRevenue;
        this.targetGrossMarginPercent = targetGrossMarginPercent;
        this.targetMaxOpex = targetMaxOpex;
        this.targetMinLiquidity = targetMinLiquidity;
        this.targetMaxOverdueAr = targetMaxOverdueAr;
        this.notes = notes != null ? notes.trim() : null;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }

    public void update(
            BigDecimal targetRevenue,
            BigDecimal targetGrossMarginPercent,
            BigDecimal targetMaxOpex,
            BigDecimal targetMinLiquidity,
            BigDecimal targetMaxOverdueAr,
            String notes
    ) {
        this.targetRevenue = targetRevenue;
        this.targetGrossMarginPercent = targetGrossMarginPercent;
        this.targetMaxOpex = targetMaxOpex;
        this.targetMinLiquidity = targetMinLiquidity;
        this.targetMaxOverdueAr = targetMaxOverdueAr;
        this.notes = notes != null ? notes.trim() : null;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public BigDecimal getTargetRevenue() {
        return targetRevenue;
    }

    public BigDecimal getTargetGrossMarginPercent() {
        return targetGrossMarginPercent;
    }

    public BigDecimal getTargetMaxOpex() {
        return targetMaxOpex;
    }

    public BigDecimal getTargetMinLiquidity() {
        return targetMinLiquidity;
    }

    public BigDecimal getTargetMaxOverdueAr() {
        return targetMaxOverdueAr;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
