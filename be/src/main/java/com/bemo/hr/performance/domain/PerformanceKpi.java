package com.bemo.hr.performance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "performance_kpis")
public class PerformanceKpi {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "cycle_id", nullable = false, length = 36)
    private String cycleId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "title_ar", nullable = false, length = 200)
    private String titleAr;

    @Column(name = "title_en", nullable = false, length = 200)
    private String titleEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private KpiCategory category;

    @Column(name = "target_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "weight_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightPercentage;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PerformanceKpi() {
    }

    public PerformanceKpi(String cycleId, String code, String titleAr, String titleEn, KpiCategory category, BigDecimal targetValue, BigDecimal weightPercentage) {
        this.id = UUID.randomUUID().toString();
        this.cycleId = cycleId;
        this.code = code;
        this.titleAr = titleAr;
        this.titleEn = titleEn;
        this.category = category != null ? category : KpiCategory.OPERATIONAL;
        this.targetValue = targetValue != null ? targetValue : new BigDecimal("100.0");
        this.weightPercentage = weightPercentage != null ? weightPercentage : new BigDecimal("20.0");
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getCycleId() {
        return cycleId;
    }

    public String getCode() {
        return code;
    }

    public String getTitleAr() {
        return titleAr;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public KpiCategory getCategory() {
        return category;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public BigDecimal getWeightPercentage() {
        return weightPercentage;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
