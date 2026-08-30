package com.bemo.hr.compliance.einvoicing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "einvoicing_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"app_id"}))
public class EinvoicingSettings {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private EinvoicingProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 30)
    private EinvoicingEnvironment environment;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected EinvoicingSettings() {
    }

    public EinvoicingSettings(EinvoicingProviderType provider, EinvoicingEnvironment environment) {
        this.id = UUID.randomUUID().toString();
        this.provider = provider;
        this.environment = environment;
    }

    public void update(EinvoicingProviderType provider, EinvoicingEnvironment environment) {
        this.provider = provider;
        this.environment = environment;
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

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public EinvoicingProviderType getProvider() { return provider; }
    public EinvoicingEnvironment getEnvironment() { return environment; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
