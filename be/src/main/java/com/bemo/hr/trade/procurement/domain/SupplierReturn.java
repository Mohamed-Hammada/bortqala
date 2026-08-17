package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "supplier_returns")
public class SupplierReturn {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "return_number", nullable = false, length = 50)
    private String returnNumber;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "purchase_order_id", nullable = false, length = 36)
    private String purchaseOrderId;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierReturnLine> lines = new ArrayList<>();

    protected SupplierReturn() {
    }

    public SupplierReturn(String returnNumber, LocalDate returnDate, String purchaseOrderId,
                          String supplierId, String warehouseId, String notes, List<SupplierReturnLine> lines) {
        this.id = UUID.randomUUID().toString();
        this.returnNumber = returnNumber.strip();
        this.returnDate = returnDate;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierId = supplierId;
        this.warehouseId = warehouseId;
        this.status = "POSTED";
        this.notes = notes;
        this.lines = new ArrayList<>();
        if (lines != null) {
            lines.forEach(this::addLine);
        }
    }

    private void addLine(SupplierReturnLine line) {
        line.attachTo(this);
        this.lines.add(line);
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public List<SupplierReturnLine> getLines() {
        return lines;
    }
}
