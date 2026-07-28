package com.bemo.hr.operations;

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
@Table(name = "unit_of_measures")
public class UnitOfMeasure {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String abbreviation;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UnitOfMeasure() {}

    public UnitOfMeasure(String name, String abbreviation, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name.strip();
        this.abbreviation = abbreviation == null || abbreviation.isBlank() ? null : abbreviation.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.active = true;
    }

    public void update(String name, String abbreviation, String description) {
        this.name = name.strip();
        this.abbreviation = abbreviation == null || abbreviation.isBlank() ? null : abbreviation.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
