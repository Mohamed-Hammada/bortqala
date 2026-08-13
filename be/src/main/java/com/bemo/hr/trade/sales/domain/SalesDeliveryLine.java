package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_delivery_lines")
public class SalesDeliveryLine {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "delivery_id", nullable = false, length = 36) private String deliveryId;
    @Column(name = "sales_order_line_id", nullable = false, length = 36) private String salesOrderLineId;
    @Column(name = "item_id", nullable = false, length = 36) private String itemId;
    @Column(nullable = false, precision = 15, scale = 4) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2) private BigDecimal unitPrice;
    @Column(name = "stock_movement_id", nullable = false, length = 36) private String stockMovementId;
    @Column(name = "unit_cogs", nullable = false, precision = 19, scale = 6) private BigDecimal unitCogs;
    @Column(name = "cogs_amount", nullable = false, precision = 19, scale = 2) private BigDecimal cogsAmount;
    @Column(name = "created_at", nullable = false) private long createdAt;

    protected SalesDeliveryLine() { }

    public SalesDeliveryLine(String deliveryId, String salesOrderLineId, String itemId, BigDecimal quantity,
                             BigDecimal unitPrice, String stockMovementId, BigDecimal unitCogs, BigDecimal cogsAmount) {
        this.id = UUID.randomUUID().toString();
        this.deliveryId = deliveryId;
        this.salesOrderLineId = salesOrderLineId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.stockMovementId = stockMovementId;
        this.unitCogs = unitCogs;
        this.cogsAmount = cogsAmount;
    }

    @PrePersist void prePersist() { createdAt = System.currentTimeMillis(); }
    public String getId() { return id; }
    public String getDeliveryId() { return deliveryId; }
    public String getSalesOrderLineId() { return salesOrderLineId; }
    public String getItemId() { return itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getStockMovementId() { return stockMovementId; }
    public BigDecimal getUnitCogs() { return unitCogs; }
    public BigDecimal getCogsAmount() { return cogsAmount; }
    public long getCreatedAt() { return createdAt; }
}
