package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_lines")
public class GoodsReceiptLine {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;
    @Column(name = "purchase_order_line_id", length = 36)
    private String purchaseOrderLineId;
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
    @Column(name = "delivered_quantity", precision = 15, scale = 2)
    private BigDecimal deliveredQuantity;
    @Column(name = "rejected_quantity", precision = 15, scale = 2)
    private BigDecimal rejectedQuantity;
    @Column(name = "deducted_quantity", precision = 15, scale = 2)
    private BigDecimal deductedQuantity;
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "location_id", length = 36)
    private String locationId;
    @Column(name = "lot_number", length = 100)
    private String lotNumber;
    @Column(name = "quality_reason", length = 500)
    private String qualityReason;

    protected GoodsReceiptLine() {
    }

    public GoodsReceiptLine(String purchaseOrderLineId, String itemId, String itemName,
                            String itemCategory, BigDecimal deliveredQuantity, BigDecimal rejectedQuantity,
                            BigDecimal deductedQuantity, BigDecimal quantity, String unitOfMeasure,
                            BigDecimal unitPrice, String locationId, String lotNumber, String qualityReason) {
        this.id = UUID.randomUUID().toString();
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.quantity = quantity;
        this.deliveredQuantity = deliveredQuantity;
        this.rejectedQuantity = rejectedQuantity;
        this.deductedQuantity = deductedQuantity;
        this.unitOfMeasure = unitOfMeasure;
        this.unitPrice = unitPrice;
        this.locationId = locationId;
        this.lotNumber = lotNumber;
        this.qualityReason = qualityReason;
    }

    public void setGoodsReceipt(GoodsReceipt goodsReceipt) {
        this.goodsReceipt = goodsReceipt;
    }

    public void attachTo(GoodsReceipt goodsReceipt) {
        this.goodsReceipt = goodsReceipt;
    }

    public void assignProject(String projectId, String wbsNodeId, String costCodeId) {
        this.projectId = projectId == null || projectId.isBlank() ? null : projectId.strip();
        this.wbsNodeId = wbsNodeId == null || wbsNodeId.isBlank() ? null : wbsNodeId.strip();
        this.costCodeId = costCodeId == null || costCodeId.isBlank() ? null : costCodeId.strip();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public GoodsReceipt getGoodsReceipt() {
        return goodsReceipt;
    }

    public String getGoodsReceiptId() {
        return goodsReceipt != null ? goodsReceipt.getId() : null;
    }

    public String getPurchaseOrderLineId() {
        return purchaseOrderLineId;
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

    public BigDecimal getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public BigDecimal getRejectedQuantity() {
        return rejectedQuantity;
    }

    public BigDecimal getDeductedQuantity() {
        return deductedQuantity;
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

    public String getLotNumber() {
        return lotNumber;
    }

    public String getQualityReason() {
        return qualityReason;
    }
}
