package com.bemo.hr.operations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stock_valuation_records")
public class StockValuationRecord {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "quantity_on_hand", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityOnHand;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected StockValuationRecord() {
    }

    public StockValuationRecord(String itemId, String warehouseId, BigDecimal quantityOnHand, BigDecimal unitCost, LocalDate asOfDate) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.warehouseId = warehouseId;
        this.quantityOnHand = quantityOnHand;
        this.unitCost = unitCost;
        this.totalValue = quantityOnHand.multiply(unitCost).setScale(2, java.math.RoundingMode.HALF_UP);
        this.asOfDate = asOfDate;
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

    public String getItemId() {
        return itemId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
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
