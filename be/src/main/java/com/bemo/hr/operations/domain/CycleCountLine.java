package com.bemo.hr.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cycle_count_lines")
public class CycleCountLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "count_id", nullable = false, length = 36)
    private String countId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "system_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal systemQuantity;

    @Column(name = "counted_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal countedQuantity;

    @Column(name = "variance_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal varianceQuantity;

    protected CycleCountLine() {}

    public CycleCountLine(String countId, String itemId, BigDecimal systemQuantity, BigDecimal countedQuantity) {
        this.id = UUID.randomUUID().toString();
        this.countId = countId;
        this.itemId = itemId;
        this.systemQuantity = systemQuantity;
        this.countedQuantity = countedQuantity;
        this.varianceQuantity = countedQuantity.subtract(systemQuantity);
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getCountId() { return countId; }
    public String getItemId() { return itemId; }
    public BigDecimal getSystemQuantity() { return systemQuantity; }
    public BigDecimal getCountedQuantity() { return countedQuantity; }
    public BigDecimal getVarianceQuantity() { return varianceQuantity; }
}
