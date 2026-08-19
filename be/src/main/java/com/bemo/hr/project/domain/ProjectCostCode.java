package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "project_cost_codes")
public class ProjectCostCode {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CostCodeCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectCostCode() {
    }

    public ProjectCostCode(String code, String name, String nameEn,
                           CostCodeCategory category, String description) {
        this.id = UUID.randomUUID().toString();
        this.code = code != null ? code.strip() : null;
        this.active = true;
        update(name, nameEn, category, description, true);
    }

    public void update(String name, String nameEn, CostCodeCategory category,
                       String description, boolean active) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cost code name is required.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Cost code category is required.");
        }
        this.name = name.strip();
        this.nameEn = nameEn != null && !nameEn.isBlank() ? nameEn.strip() : null;
        this.category = category;
        this.description = description != null && !description.isBlank() ? description.strip() : null;
        this.active = active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public CostCodeCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
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
}
