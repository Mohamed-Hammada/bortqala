package com.bemo.hr.operations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_headers")
public class StockTransferHeader {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "transfer_number", nullable = false, length = 50)
    private String transferNumber;

    @Column(name = "source_warehouse_id", nullable = false, length = 36)
    private String sourceWarehouseId;

    @Column(name = "target_warehouse_id", nullable = false, length = 36)
    private String targetWarehouseId;

    @Column(name = "source_branch_id", length = 36)
    private String sourceBranchId;

    @Column(name = "target_branch_id", length = 36)
    private String targetBranchId;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "carrier_name", length = 150)
    private String carrierName;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 50)
    private String driverPhone;

    @Column(name = "vehicle_plate", length = 50)
    private String vehiclePlate;

    @Column(name = "waybill_number", length = 100)
    private String waybillNumber;

    @Column(name = "dispatched_at")
    private Long dispatchedAt;

    @Column(name = "dispatched_by", length = 100)
    private String dispatchedBy;

    @Column(name = "received_at")
    private Long receivedAt;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "has_discrepancy", nullable = false)
    private boolean hasDiscrepancy = false;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "intercompany_transaction_id", length = 36)
    private String intercompanyTransactionId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected StockTransferHeader() {
    }

    public StockTransferHeader(String transferNumber, String sourceWarehouseId, String targetWarehouseId, LocalDate transferDate) {
        this(transferNumber, sourceWarehouseId, targetWarehouseId, null, null, transferDate);
    }

    public StockTransferHeader(String transferNumber, String sourceWarehouseId, String targetWarehouseId,
                               String sourceBranchId, String targetBranchId, LocalDate transferDate) {
        this.id = UUID.randomUUID().toString();
        this.transferNumber = transferNumber;
        this.sourceWarehouseId = sourceWarehouseId;
        this.targetWarehouseId = targetWarehouseId;
        this.sourceBranchId = sourceBranchId;
        this.targetBranchId = targetBranchId;
        this.transferDate = transferDate;
        this.status = Status.DRAFT;
    }

    public void setBranchIds(String sourceBranchId, String targetBranchId) {
        this.sourceBranchId = sourceBranchId;
        this.targetBranchId = targetBranchId;
    }

    public void ship() {
        dispatch(null, null, null, null, null, null, "SYSTEM");
    }

    public void dispatch(String carrierName, String driverName, String driverPhone,
                         String vehiclePlate, String waybillNumber, String notes, String actor) {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT stock transfers can be shipped/dispatched");
        }
        this.status = Status.SHIPPED;
        this.carrierName = carrierName == null || carrierName.isBlank() ? null : carrierName.strip();
        this.driverName = driverName == null || driverName.isBlank() ? null : driverName.strip();
        this.driverPhone = driverPhone == null || driverPhone.isBlank() ? null : driverPhone.strip();
        this.vehiclePlate = vehiclePlate == null || vehiclePlate.isBlank() ? null : vehiclePlate.strip();
        this.waybillNumber = waybillNumber == null || waybillNumber.isBlank() ? null : waybillNumber.strip();
        this.notes = notes == null || notes.isBlank() ? null : notes.strip();
        this.dispatchedAt = System.currentTimeMillis();
        this.dispatchedBy = actor != null ? actor : "SYSTEM";
    }

    public void receive() {
        receiveWithInspection("SYSTEM", false);
    }

    public void receiveWithInspection(String actor, boolean hasDiscrepancy) {
        if (this.status != Status.SHIPPED) {
            throw new IllegalStateException("Only SHIPPED stock transfers can be received");
        }
        this.status = Status.RECEIVED;
        this.hasDiscrepancy = hasDiscrepancy;
        this.receivedAt = System.currentTimeMillis();
        this.receivedBy = actor != null ? actor : "SYSTEM";
    }

    public void cancel() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT stock transfers can be cancelled");
        }
        this.status = Status.CANCELLED;
    }

    public void linkIntercompanyTransaction(String intercompanyTransactionId) {
        this.intercompanyTransactionId = intercompanyTransactionId;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getTransferNumber() {
        return transferNumber;
    }

    public String getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public String getTargetWarehouseId() {
        return targetWarehouseId;
    }

    public String getSourceBranchId() {
        return sourceBranchId;
    }

    public String getTargetBranchId() {
        return targetBranchId;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public Status getStatus() {
        return status;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public String getWaybillNumber() {
        return waybillNumber;
    }

    public Long getDispatchedAt() {
        return dispatchedAt;
    }

    public String getDispatchedBy() {
        return dispatchedBy;
    }

    public Long getReceivedAt() {
        return receivedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public boolean isHasDiscrepancy() {
        return hasDiscrepancy;
    }

    public String getNotes() {
        return notes;
    }

    public String getIntercompanyTransactionId() {
        return intercompanyTransactionId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        DRAFT, SHIPPED, RECEIVED, CANCELLED
    }
}
