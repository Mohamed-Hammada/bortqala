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
import java.time.Instant;
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

    @Column(name = "target_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetQuantity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProductionOrder() {}

    public ProductionOrder(String orderNumber, String bomId, BigDecimal targetQuantity, LocalDate startDate) {
        this.id = UUID.randomUUID().toString();
        this.orderNumber = orderNumber.strip();
        this.bomId = bomId;
        this.targetQuantity = targetQuantity == null ? BigDecimal.ONE : targetQuantity;
        this.startDate = startDate;
        this.status = Status.PLANNED;
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
    public BigDecimal getTargetQuantity() { return targetQuantity; }
    public LocalDate getStartDate() { return startDate; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
