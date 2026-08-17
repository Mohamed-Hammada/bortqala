package com.bemo.hr.operations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "warehouse_bins")
public class WarehouseBin {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "bin_code", nullable = false, length = 50)
    private String binCode;

    @Column(length = 50)
    private String aisle;

    @Column(length = 50)
    private String rack;

    @Column(length = 50)
    private String shelf;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WarehouseBin() {
    }

    public WarehouseBin(String warehouseId, String binCode, String aisle, String rack, String shelf) {
        this.id = UUID.randomUUID().toString();
        this.warehouseId = warehouseId;
        this.binCode = binCode;
        this.aisle = aisle;
        this.rack = rack;
        this.shelf = shelf;
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

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getBinCode() {
        return binCode;
    }

    public String getAisle() {
        return aisle;
    }

    public String getRack() {
        return rack;
    }

    public String getShelf() {
        return shelf;
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
