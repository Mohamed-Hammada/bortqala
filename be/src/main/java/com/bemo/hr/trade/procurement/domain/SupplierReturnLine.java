package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "supplier_return_lines")
public class SupplierReturnLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_return_id", nullable = false)
    private SupplierReturn supplierReturn;

    @Column(name = "purchase_order_line_id", length = 36)
    private String purchaseOrderLineId;

    @Column(name = "item_id", length = 36)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "item_category", length = 100)
    private String itemCategory;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "location_id", length = 36)
    private String locationId;

    @Column(name = "reason", length = 500)
    private String reason;

    protected SupplierReturnLine() {
    }

    public SupplierReturnLine(String purchaseOrderLineId, String itemId, String itemName,
                              String itemCategory, BigDecimal quantity, String unitOfMeasure,
                              BigDecimal unitPrice, String locationId, String reason) {
        this.id = UUID.randomUUID().toString();
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.unitPrice = unitPrice;
        this.locationId = locationId;
        this.reason = reason;
    }

    void attachTo(SupplierReturn supplierReturn) {
        this.supplierReturn = supplierReturn;
    }

    public String getId() {
        return id;
    }

    public String getSupplierReturnId() {
        return supplierReturn == null ? null : supplierReturn.getId();
    }

    public String getPurchaseOrderLineId() {
        return purchaseOrderLineId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getReason() {
        return reason;
    }
}
