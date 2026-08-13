package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_return_lines")
public class CustomerReturnLine {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "return_id", nullable = false, length = 36) private String returnId;
    @Column(name = "delivery_line_id", nullable = false, length = 36) private String deliveryLineId;
    @Column(name = "item_id", nullable = false, length = 36) private String itemId;
    @Column(nullable = false, precision = 15, scale = 4) private BigDecimal quantity;
    @Column(nullable = false, length = 20) private String disposition;
    @Column(name = "stock_movement_id", nullable = false, length = 36) private String stockMovementId;
    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2) private BigDecimal creditAmount;
    @Column(name = "cogs_amount", nullable = false, precision = 19, scale = 2) private BigDecimal cogsAmount;
    @Column(name = "created_at", nullable = false) private long createdAt;

    protected CustomerReturnLine() { }

    public CustomerReturnLine(String returnId, String deliveryLineId, String itemId, BigDecimal quantity,
                              String disposition, String stockMovementId, BigDecimal creditAmount, BigDecimal cogsAmount) {
        this.id = UUID.randomUUID().toString();
        this.returnId = returnId;
        this.deliveryLineId = deliveryLineId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.disposition = disposition;
        this.stockMovementId = stockMovementId;
        this.creditAmount = creditAmount;
        this.cogsAmount = cogsAmount;
    }

    @PrePersist void prePersist() { createdAt = System.currentTimeMillis(); }
    public String getId() { return id; }
    public String getReturnId() { return returnId; }
    public String getDeliveryLineId() { return deliveryLineId; }
    public String getItemId() { return itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getDisposition() { return disposition; }
    public String getStockMovementId() { return stockMovementId; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public BigDecimal getCogsAmount() { return cogsAmount; }
    public long getCreatedAt() { return createdAt; }
}
