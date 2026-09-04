package com.bemo.hr.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_lines")
public class StockTransferLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "shipped_quantity", precision = 15, scale = 4)
    private BigDecimal shippedQuantity;

    @Column(name = "received_quantity", precision = 15, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "damaged_quantity", precision = 15, scale = 4)
    private BigDecimal damagedQuantity = BigDecimal.ZERO;

    @Column(name = "lost_quantity", precision = 15, scale = 4)
    private BigDecimal lostQuantity = BigDecimal.ZERO;

    @Column(name = "discrepancy_reason", length = 50)
    private String discrepancyReason;

    @Column(name = "discrepancy_notes", length = 255)
    private String discrepancyNotes;

    protected StockTransferLine() {
    }

    public StockTransferLine(String transferId, String itemId, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.transferId = transferId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.shippedQuantity = quantity;
    }

    public void updateReceipt(BigDecimal receivedQty, BigDecimal damagedQty, BigDecimal lostQty,
                              String discrepancyReason, String discrepancyNotes) {
        this.receivedQuantity = receivedQty != null ? receivedQty : BigDecimal.ZERO;
        this.damagedQuantity = damagedQty != null ? damagedQty : BigDecimal.ZERO;
        this.lostQuantity = lostQty != null ? lostQty : BigDecimal.ZERO;
        this.discrepancyReason = discrepancyReason == null || discrepancyReason.isBlank() ? null : discrepancyReason.strip();
        this.discrepancyNotes = discrepancyNotes == null || discrepancyNotes.isBlank() ? null : discrepancyNotes.strip();
    }

    public void setShippedQuantity(BigDecimal shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getShippedQuantity() {
        return shippedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getDamagedQuantity() {
        return damagedQuantity;
    }

    public BigDecimal getLostQuantity() {
        return lostQuantity;
    }

    public String getDiscrepancyReason() {
        return discrepancyReason;
    }

    public String getDiscrepancyNotes() {
        return discrepancyNotes;
    }
}
