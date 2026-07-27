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
@Table(name = "warehouses")
public class Warehouse {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "branch_id", nullable = false, length = 36)
    private String branchId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Warehouse() {}

    public Warehouse(String branchId, String code, String name, String location, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(branchId, code, name, location, active);
    }

    public void update(String branchId, String code, String name, String location, boolean active) {
        this.branchId = branchId;
        this.code = code.strip();
        this.name = name.strip();
        this.location = location == null ? null : location.strip();
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getBranchId() { return branchId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
