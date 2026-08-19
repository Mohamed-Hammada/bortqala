package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_quotation_lines")
public class SalesQuotationLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "quotation_id", nullable = false, length = 36)
    private String quotationId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "notes", length = 500)
    private String notes;

    protected SalesQuotationLine() {
    }

    public SalesQuotationLine(String quotationId, String itemId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal lineTotal, String notes) {
        this.id = UUID.randomUUID().toString();
        this.quotationId = quotationId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.lineTotal = lineTotal;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getQuotationId() {
        return quotationId;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public String getNotes() {
        return notes;
    }
}
