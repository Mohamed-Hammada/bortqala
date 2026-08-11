package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_requisition_lines")
public class PurchaseRequisitionLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "requisition_id", nullable = false, length = 36)
    private String requisitionId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "requested_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal requestedQuantity;

    @Column(name = "unit_price_estimate", precision = 15, scale = 2)
    private BigDecimal unitPriceEstimate;

    @Column(length = 500)
    private String notes;

    protected PurchaseRequisitionLine() {}

    public PurchaseRequisitionLine(String requisitionId, String itemId, String itemName, BigDecimal requestedQuantity, BigDecimal unitPriceEstimate, String notes) {
        this.id = UUID.randomUUID().toString();
        this.requisitionId = requisitionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.requestedQuantity = requestedQuantity;
        this.unitPriceEstimate = unitPriceEstimate;
        this.notes = notes;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRequisitionId() { return requisitionId; }
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public BigDecimal getRequestedQuantity() { return requestedQuantity; }
    public BigDecimal getUnitPriceEstimate() { return unitPriceEstimate; }
    public String getNotes() { return notes; }
}
