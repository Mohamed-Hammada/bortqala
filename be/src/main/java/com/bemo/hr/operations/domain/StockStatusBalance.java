package com.bemo.hr.operations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_status_balances")
public class StockStatusBalance {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;
    @Column(name = "bin_id", nullable = false, length = 36)
    private String binId;
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected StockStatusBalance() {
    }

    public StockStatusBalance(String warehouseId, String binId, String itemId, Status status, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.warehouseId = warehouseId;
        this.binId = binId == null ? "" : binId;
        this.itemId = itemId;
        this.status = status;
        this.quantity = quantity;
    }

    public void adjustQuantity(BigDecimal delta) {
        BigDecimal adjusted = this.quantity.add(delta);
        if (adjusted.signum() < 0) {
            throw new IllegalArgumentException("Stock status balance cannot be negative");
        }
        this.quantity = adjusted;
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

    public String getBinId() {
        return binId;
    }

    public String getItemId() {
        return itemId;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getQuantity() {
        return quantity;
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

    public enum Status {
        AVAILABLE, QUARANTINE, BLOCKED
    }
}
