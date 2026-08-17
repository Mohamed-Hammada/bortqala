package com.bemo.hr.operations;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "item_categories")
public class ItemCategory {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ItemCategory() {
    }

    public ItemCategory(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.active = true;
    }

    public void update(String name, String description) {
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
