package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "production_orders")
public class ProductionOrder {

    public enum Status {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "bom_id", nullable = false, length = 36)
    private String bomId;

    @Column(name = "finished_item_id", length = 36)
    private String finishedItemId;

    @Column(name = "bom_revision", length = 20)
    private String bomRevision;

    @Column(name = "target_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetQuantity;

    @Column(name = "actual_output_quantity", precision = 12, scale = 2)
    private BigDecimal actualOutputQuantity;

    @Column(name = "scrap_quantity", precision = 12, scale = 2)
    private BigDecimal scrapQuantity;

    @Column(name = "actual_material_cost", precision = 15, scale = 2)
    private BigDecimal actualMaterialCost;

    @Column(name = "actual_unit_cost", precision = 15, scale = 2)
    private BigDecimal actualUnitCost;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProductionOrder() {}

    public ProductionOrder(String orderNumber, String bomId, String finishedItemId,
                           String bomRevision, BigDecimal targetQuantity, LocalDate startDate, String notes) {
        this.id = UUID.randomUUID().toString();
        this.orderNumber = orderNumber.strip();
        this.bomId = bomId;
        this.finishedItemId = finishedItemId;
        this.bomRevision = bomRevision == null || bomRevision.isBlank() ? "v1.0" : bomRevision.strip();
        this.targetQuantity = targetQuantity == null ? BigDecimal.ONE : targetQuantity;
        this.startDate = startDate;
        this.notes = notes == null ? null : notes.strip();
        this.status = Status.PLANNED;
    }

    public void start() {
        this.status = Status.IN_PROGRESS;
    }

    public void complete(BigDecimal actualOutputQuantity, BigDecimal scrapQuantity,
                         BigDecimal actualMaterialCost, BigDecimal actualUnitCost,
                         LocalDate completionDate, String notes) {
        this.status = Status.COMPLETED;
        this.actualOutputQuantity = actualOutputQuantity;
        this.scrapQuantity = scrapQuantity == null ? BigDecimal.ZERO : scrapQuantity;
        this.actualMaterialCost = actualMaterialCost;
        this.actualUnitCost = actualUnitCost;
        this.completionDate = completionDate;
        if (notes != null && !notes.isBlank()) {
            this.notes = notes.strip();
        }
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getBomId() { return bomId; }
    public String getFinishedItemId() { return finishedItemId; }
    public String getBomRevision() { return bomRevision; }
    public BigDecimal getTargetQuantity() { return targetQuantity; }
    public BigDecimal getActualOutputQuantity() { return actualOutputQuantity; }
    public BigDecimal getScrapQuantity() { return scrapQuantity; }
    public BigDecimal getActualMaterialCost() { return actualMaterialCost; }
    public BigDecimal getActualUnitCost() { return actualUnitCost; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getCompletionDate() { return completionDate; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
