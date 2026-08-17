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
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;
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
        this.id = UUID.randomUUID().toString();
        this.transferNumber = transferNumber;
        this.sourceWarehouseId = sourceWarehouseId;
        this.targetWarehouseId = targetWarehouseId;
        this.transferDate = transferDate;
        this.status = Status.DRAFT;
    }

    public void ship() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT stock transfers can be shipped");
        }
        this.status = Status.SHIPPED;
    }

    public void receive() {
        if (this.status != Status.SHIPPED) {
            throw new IllegalStateException("Only SHIPPED stock transfers can be received");
        }
        this.status = Status.RECEIVED;
    }

    public void cancel() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT stock transfers can be cancelled");
        }
        this.status = Status.CANCELLED;
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

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public Status getStatus() {
        return status;
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
