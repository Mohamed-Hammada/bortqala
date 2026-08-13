package com.bemo.hr.operations.domain;

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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
public class StockReservation {

    public enum Status {
        ACTIVE, FULFILLED, CANCELLED, EXPIRED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "reservation_number", nullable = false, length = 50)
    private String reservationNumber;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 36)
    private String sourceId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected StockReservation() {}

    public StockReservation(String reservationNumber, String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal reservedQuantity) {
        this.id = UUID.randomUUID().toString();
        this.reservationNumber = reservationNumber;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.itemId = itemId;
        this.warehouseId = warehouseId;
        this.reservedQuantity = reservedQuantity;
        this.status = Status.ACTIVE;
    }

    public void fulfill() {
        if (status == Status.FULFILLED) return;
        if (status != Status.ACTIVE) throw new IllegalStateException("Only active reservations can be fulfilled");
        this.status = Status.FULFILLED;
    }

    public void cancel() {
        if (status == Status.CANCELLED) return;
        if (status != Status.ACTIVE) throw new IllegalStateException("Only active reservations can be cancelled");
        this.status = Status.CANCELLED;
    }

    public void expire() {
        if (status == Status.EXPIRED) return;
        if (status != Status.ACTIVE) throw new IllegalStateException("Only active reservations can expire");
        this.status = Status.EXPIRED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getReservationNumber() { return reservationNumber; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public String getItemId() { return itemId; }
    public String getWarehouseId() { return warehouseId; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
