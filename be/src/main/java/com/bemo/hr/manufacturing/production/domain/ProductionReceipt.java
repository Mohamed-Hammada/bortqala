package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "production_receipts")
public class ProductionReceipt {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "production_order_id", nullable = false, length = 36)
    private String productionOrderId;

    @Column(name = "finished_item_id", nullable = false, length = 36)
    private String finishedItemId;

    @Column(name = "received_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ProductionReceipt() {}

    public ProductionReceipt(String receiptNumber, String productionOrderId, String finishedItemId, BigDecimal receivedQuantity, LocalDate receiptDate, String warehouseId) {
        this.id = UUID.randomUUID().toString();
        this.receiptNumber = receiptNumber;
        this.productionOrderId = productionOrderId;
        this.finishedItemId = finishedItemId;
        this.receivedQuantity = receivedQuantity;
        this.receiptDate = receiptDate;
        this.warehouseId = warehouseId;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getReceiptNumber() { return receiptNumber; }
    public String getProductionOrderId() { return productionOrderId; }
    public String getFinishedItemId() { return finishedItemId; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public String getWarehouseId() { return warehouseId; }
    public long getCreatedAt() { return createdAt; }
}
