package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "purchase_order_id", nullable = false, length = 36)
    private String purchaseOrderId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;

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

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected PurchaseOrderLine() {
    }

    public PurchaseOrderLine(String purchaseOrderId, String itemId, String itemName, String itemCategory,
                             BigDecimal quantity, String unitOfMeasure, BigDecimal unitPrice) {
        this.id = UUID.randomUUID().toString();
        this.purchaseOrderId = purchaseOrderId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity.multiply(unitPrice);
        this.createdAt = System.currentTimeMillis();
    }

    public void assignProject(String projectId, String wbsNodeId, String costCodeId) {
        this.projectId = projectId == null || projectId.isBlank() ? null : projectId.strip();
        this.wbsNodeId = wbsNodeId == null || wbsNodeId.isBlank() ? null : wbsNodeId.strip();
        this.costCodeId = costCodeId == null || costCodeId.isBlank() ? null : costCodeId.strip();
    }

    public void update(String itemId, String itemName, String itemCategory,
                       BigDecimal quantity, String unitOfMeasure, BigDecimal unitPrice) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity.multiply(unitPrice);
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getWbsNodeId() {
        return wbsNodeId;
    }

    public String getCostCodeId() {
        return costCodeId;
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

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
