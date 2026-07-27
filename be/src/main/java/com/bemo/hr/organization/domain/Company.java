package com.bemo.hr.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "commercial_registry", length = 100)
    private String commercialRegistry;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Company() {}

    public Company(String code, String name, String taxNumber, String commercialRegistry, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, taxNumber, commercialRegistry, active);
    }

    public void update(String code, String name, String taxNumber, String commercialRegistry, boolean active) {
        this.code = code.strip();
        this.name = name.strip();
        this.taxNumber = taxNumber == null ? null : taxNumber.strip();
        this.commercialRegistry = commercialRegistry == null ? null : commercialRegistry.strip();
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTaxNumber() { return taxNumber; }
    public String getCommercialRegistry() { return commercialRegistry; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
