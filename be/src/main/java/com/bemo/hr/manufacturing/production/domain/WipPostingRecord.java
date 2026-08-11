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
@Table(name = "wip_posting_records")
public class WipPostingRecord {

    public enum Status {
        DRAFT, POSTED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;

    @Column(name = "work_center_id", nullable = false, length = 36)
    private String workCenterId;

    @Column(name = "labor_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborHours;

    @Column(name = "machine_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal machineHours;

    @Column(name = "total_wip_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalWipCost;

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

    protected WipPostingRecord() {}

    public WipPostingRecord(String workOrderId, String workCenterId, BigDecimal laborHours, BigDecimal machineHours, BigDecimal totalWipCost) {
        this.id = UUID.randomUUID().toString();
        this.workOrderId = workOrderId;
        this.workCenterId = workCenterId;
        this.laborHours = laborHours;
        this.machineHours = machineHours;
        this.totalWipCost = totalWipCost;
        this.status = Status.POSTED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getWorkOrderId() { return workOrderId; }
    public String getWorkCenterId() { return workCenterId; }
    public BigDecimal getLaborHours() { return laborHours; }
    public BigDecimal getMachineHours() { return machineHours; }
    public BigDecimal getTotalWipCost() { return totalWipCost; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
