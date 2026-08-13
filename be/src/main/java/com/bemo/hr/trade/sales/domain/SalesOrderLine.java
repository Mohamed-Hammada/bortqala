package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_order_lines")
public class SalesOrderLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "sales_order_id", nullable = false, length = 36)
    private String salesOrderId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "ordered_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal orderedQuantity;

    @Column(name = "delivered_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal deliveredQuantity = BigDecimal.ZERO;

    @Column(name = "invoiced_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal invoicedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(name = "net_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    protected SalesOrderLine() {}

    public SalesOrderLine(String salesOrderId, String itemId, String itemName, BigDecimal orderedQuantity, BigDecimal unitPrice, BigDecimal discountRate) {
        this.id = UUID.randomUUID().toString();
        this.salesOrderId = salesOrderId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.orderedQuantity = orderedQuantity;
        this.unitPrice = unitPrice;
        this.discountRate = discountRate == null ? BigDecimal.ZERO : discountRate;
        BigDecimal discountFactor = BigDecimal.ONE.subtract(this.discountRate.divide(new BigDecimal("100")));
        this.netPrice = unitPrice.multiply(discountFactor);
        this.lineTotal = this.netPrice.multiply(orderedQuantity);
        this.deliveredQuantity = BigDecimal.ZERO;
        this.invoicedQuantity = BigDecimal.ZERO;
    }

    public void recordDelivery(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || deliveredQuantity.add(quantity).compareTo(orderedQuantity) > 0) {
            throw new IllegalArgumentException("Delivered quantity exceeds the open order quantity");
        }
        this.deliveredQuantity = this.deliveredQuantity.add(quantity);
    }

    public void recordInvoice(BigDecimal quantity) {
        this.invoicedQuantity = this.invoicedQuantity.add(quantity);
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSalesOrderId() { return salesOrderId; }
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public BigDecimal getDeliveredQuantity() { return deliveredQuantity; }
    public BigDecimal getInvoicedQuantity() { return invoicedQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public BigDecimal getNetPrice() { return netPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
