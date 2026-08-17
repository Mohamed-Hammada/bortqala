package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "supplier_quote_lines")
public class SupplierQuoteLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "quote_id", nullable = false, length = 36)
    private String quoteId;

    @Column(name = "rfq_line_id", length = 36)
    private String rfqLineId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(length = 20)
    private String uom;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice;

    protected SupplierQuoteLine() {
    }

    public SupplierQuoteLine(String quoteId, String rfqLineId, String itemId, String description, BigDecimal quantity, String uom, BigDecimal unitPrice) {
        this.id = UUID.randomUUID().toString();
        this.quoteId = quoteId;
        this.rfqLineId = rfqLineId;
        this.itemId = itemId;
        this.description = description;
        this.quantity = quantity;
        this.uom = uom;
        this.unitPrice = unitPrice;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getRfqLineId() {
        return rfqLineId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUom() {
        return uom;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
