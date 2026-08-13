package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bom_snapshots")
public class BomSnapshot {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "production_order_id", nullable = false, length = 36)
    private String productionOrderId;

    @Column(name = "bom_id", nullable = false, length = 36)
    private String bomId;

    @Column(name = "bom_version", nullable = false)
    private int bomVersion;

    @Column(name = "component_item_id", nullable = false, length = 36)
    private String componentItemId;

    @Column(name = "required_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal requiredQuantity;

    @Column(name = "standard_unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal standardUnitCost;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected BomSnapshot() {}

    public BomSnapshot(String productionOrderId, String bomId, int bomVersion, String componentItemId,
                       BigDecimal requiredQuantity, BigDecimal standardUnitCost) {
        this.id = UUID.randomUUID().toString();
        this.productionOrderId = productionOrderId;
        this.bomId = bomId;
        this.bomVersion = bomVersion;
        this.componentItemId = componentItemId;
        this.requiredQuantity = requiredQuantity;
        this.standardUnitCost = standardUnitCost;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getProductionOrderId() { return productionOrderId; }
    public String getBomId() { return bomId; }
    public int getBomVersion() { return bomVersion; }
    public String getComponentItemId() { return componentItemId; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public BigDecimal getStandardUnitCost() { return standardUnitCost; }
    public long getCreatedAt() { return createdAt; }
}
