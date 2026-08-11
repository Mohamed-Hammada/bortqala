package com.bemo.hr.budget.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "budget_versions")
public class BudgetVersion {

    public enum Status {
        DRAFT, ACTIVE, SUPERSEDED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "version_code", nullable = false, length = 50)
    private String versionCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BudgetVersion() {}

    public BudgetVersion(String versionCode, String name, int fiscalYear) {
        this.id = UUID.randomUUID().toString();
        this.versionCode = versionCode;
        this.name = name;
        this.fiscalYear = fiscalYear;
        this.status = Status.DRAFT;
    }

    public void activate() {
        this.status = Status.ACTIVE;
    }

    public void supersede() {
        this.status = Status.SUPERSEDED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getVersionCode() { return versionCode; }
    public String getName() { return name; }
    public int getFiscalYear() { return fiscalYear; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
