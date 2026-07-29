package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt {

    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "grn_number", nullable = false, length = 50) private String grnNumber;
    @Column(name = "receipt_date", nullable = false) private LocalDate receiptDate;
    @Column(name = "purchase_order_id", nullable = false, length = 36) private String purchaseOrderId;
    @Column(name = "supplier_id", nullable = false, length = 36) private String supplierId;
    @Column(name = "warehouse_id", length = 36) private String warehouseId;
    @Column(name = "status", nullable = false, length = 20) private String status;
    @Column(length = 500) private String notes;
    @Column(name = "created_at", nullable = false) private long createdAt;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "goods_receipt_id")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    protected GoodsReceipt() {}

    public GoodsReceipt(String grnNumber, LocalDate receiptDate, String purchaseOrderId,
                        String supplierId, String warehouseId, String notes, List<GoodsReceiptLine> lines) {
        this.id = UUID.randomUUID().toString();
        this.grnNumber = grnNumber.strip();
        this.receiptDate = receiptDate;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierId = supplierId;
        this.warehouseId = warehouseId;
        this.status = "POSTED";
        this.notes = notes;
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    @PrePersist void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getGrnNumber() { return grnNumber; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public String getPurchaseOrderId() { return purchaseOrderId; }
    public String getSupplierId() { return supplierId; }
    public String getWarehouseId() { return warehouseId; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public long getCreatedAt() { return createdAt; }
    public List<GoodsReceiptLine> getLines() { return lines; }
}
