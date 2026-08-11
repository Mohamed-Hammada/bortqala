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
import java.util.UUID;

@Entity
@Table(name = "production_variance_closes")
public class ProductionVarianceClose {

    public enum Status {
        DRAFT, CLOSED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;

    @Column(name = "standard_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal standardCost;

    @Column(name = "actual_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "variance_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.CLOSED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProductionVarianceClose() {}

    public ProductionVarianceClose(String workOrderId, BigDecimal standardCost, BigDecimal actualCost) {
        this.id = UUID.randomUUID().toString();
        this.workOrderId = workOrderId;
        this.standardCost = standardCost;
        this.actualCost = actualCost;
        this.varianceCost = actualCost.subtract(standardCost);
        this.status = Status.CLOSED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getWorkOrderId() { return workOrderId; }
    public BigDecimal getStandardCost() { return standardCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public BigDecimal getVarianceCost() { return varianceCost; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
