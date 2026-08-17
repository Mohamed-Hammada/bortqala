package com.bemo.hr.operations;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_cost_layers")
@Getter
public class InventoryCostLayer {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Column(name = "source_movement_id", nullable = false, length = 36)
    private String sourceMovementId;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(name = "initial_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialQuantity;
    @Column(name = "remaining_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingQuantity;
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitCost;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryCostLayer() {
    }

    public InventoryCostLayer(String itemId, String sourceMovementId, Instant receivedAt,
                              BigDecimal quantity, BigDecimal unitCost) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.sourceMovementId = sourceMovementId;
        this.receivedAt = receivedAt;
        this.initialQuantity = quantity;
        this.remainingQuantity = quantity;
        this.unitCost = unitCost;
    }

    public BigDecimal consume(BigDecimal requested) {
        BigDecimal consumed = requested.min(remainingQuantity);
        remainingQuantity = remainingQuantity.subtract(consumed);
        return consumed;
    }

    public void revalue(BigDecimal newUnitCost) {
        this.unitCost = newUnitCost;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
