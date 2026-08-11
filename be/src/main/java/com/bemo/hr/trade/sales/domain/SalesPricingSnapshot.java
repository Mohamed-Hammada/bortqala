package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_pricing_snapshots")
public class SalesPricingSnapshot {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "sales_order_id", nullable = false, length = 36)
    private String salesOrderId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "net_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPrice;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected SalesPricingSnapshot() {}

    public SalesPricingSnapshot(String salesOrderId, String itemId, BigDecimal unitPrice, BigDecimal discountRate, BigDecimal netPrice) {
        this.id = UUID.randomUUID().toString();
        this.salesOrderId = salesOrderId;
        this.itemId = itemId;
        this.unitPrice = unitPrice;
        this.discountRate = discountRate;
        this.netPrice = netPrice;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSalesOrderId() { return salesOrderId; }
    public String getItemId() { return itemId; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public BigDecimal getNetPrice() { return netPrice; }
    public long getCreatedAt() { return createdAt; }
}
