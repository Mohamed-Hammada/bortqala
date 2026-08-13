package com.bemo.hr.inventory.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 36)
    private String sourceId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Version
    private Long version;

    public enum Status {
        ACTIVE, FULFILLED, RELEASED
    }

    protected InventoryReservation() {}

    public InventoryReservation(String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.itemId = itemId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.status = Status.ACTIVE;
    }

    public void release() {
        this.status = Status.RELEASED;
    }

    public void fulfill() {
        this.status = Status.FULFILLED;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public String getItemId() { return itemId; }
    public String getWarehouseId() { return warehouseId; }
    public BigDecimal getQuantity() { return quantity; }
    public Status getStatus() { return status; }
    public Long getVersion() { return version; }
}
