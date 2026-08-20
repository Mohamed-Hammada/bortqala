package com.bemo.hr.analytics.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "executive_kpi_snapshots")
public class ExecutiveKpiSnapshot {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "snapshot_date", nullable = false)
    private long snapshotDate;

    @Column(name = "period_key", length = 32, nullable = false)
    private String periodKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32, nullable = false)
    private KpiCategory category;

    @Column(name = "kpi_key", length = 64, nullable = false)
    private String kpiKey;

    @Column(name = "target_value", precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "actual_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal actualValue;

    @Column(name = "variance_value", precision = 19, scale = 4)
    private BigDecimal varianceValue;

    @Column(name = "variance_percent", precision = 7, scale = 2)
    private BigDecimal variancePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "trend_direction", length = 16, nullable = false)
    private TrendDirection trendDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", length = 32, nullable = false)
    private ReconciliationStatus reconciliationStatus;

    @Column(name = "drilldown_url", length = 255)
    private String drilldownUrl;

    @Column(name = "metadata_json", length = 2000)
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected ExecutiveKpiSnapshot() {}

    public ExecutiveKpiSnapshot(
            String periodKey,
            KpiCategory category,
            String kpiKey,
            BigDecimal targetValue,
            BigDecimal actualValue,
            BigDecimal varianceValue,
            BigDecimal variancePercent,
            TrendDirection trendDirection,
            ReconciliationStatus reconciliationStatus,
            String drilldownUrl,
            String metadataJson
    ) {
        this.id = UUID.randomUUID().toString();
        this.periodKey = Objects.requireNonNull(periodKey, "periodKey cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.kpiKey = Objects.requireNonNull(kpiKey, "kpiKey cannot be null");
        this.actualValue = Objects.requireNonNull(actualValue, "actualValue cannot be null");
        this.targetValue = targetValue;
        this.varianceValue = varianceValue;
        this.variancePercent = variancePercent;
        this.trendDirection = trendDirection != null ? trendDirection : TrendDirection.STABLE;
        this.reconciliationStatus = reconciliationStatus != null ? reconciliationStatus : ReconciliationStatus.RECONCILED;
        this.drilldownUrl = drilldownUrl;
        this.metadataJson = metadataJson;
        long now = Instant.now().toEpochMilli();
        this.snapshotDate = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public long getSnapshotDate() { return snapshotDate; }
    public String getPeriodKey() { return periodKey; }
    public KpiCategory getCategory() { return category; }
    public String getKpiKey() { return kpiKey; }
    public BigDecimal getTargetValue() { return targetValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public BigDecimal getVarianceValue() { return varianceValue; }
    public BigDecimal getVariancePercent() { return variancePercent; }
    public TrendDirection getTrendDirection() { return trendDirection; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public String getDrilldownUrl() { return drilldownUrl; }
    public String getMetadataJson() { return metadataJson; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
