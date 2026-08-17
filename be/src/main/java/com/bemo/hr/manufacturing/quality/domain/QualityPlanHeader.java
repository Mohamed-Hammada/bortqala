package com.bemo.hr.manufacturing.quality.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "quality_plan_headers")
public class QualityPlanHeader {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_category", nullable = false, length = 30)
    private TargetCategory targetCategory;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected QualityPlanHeader() {
    }

    public QualityPlanHeader(String planCode, String name, String itemId, TargetCategory targetCategory) {
        this.id = UUID.randomUUID().toString();
        this.planCode = planCode.strip();
        this.name = name.strip();
        this.itemId = itemId;
        this.targetCategory = targetCategory;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public String getName() {
        return name;
    }

    public String getItemId() {
        return itemId;
    }

    public TargetCategory getTargetCategory() {
        return targetCategory;
    }

    public boolean isActive() {
        return active;
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

    public enum TargetCategory {
        INCOMING_INSPECTION, IN_PROCESS, FINAL_INSPECTION
    }
}
