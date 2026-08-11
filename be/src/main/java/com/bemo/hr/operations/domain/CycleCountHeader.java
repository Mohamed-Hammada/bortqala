package com.bemo.hr.operations.domain;

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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cycle_count_headers")
public class CycleCountHeader {

    public enum Status {
        DRAFT, IN_PROGRESS, SUBMITTED, ADJUSTED, CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "count_number", nullable = false, length = 50)
    private String countNumber;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

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

    protected CycleCountHeader() {}

    public CycleCountHeader(String countNumber, String warehouseId, LocalDate countDate) {
        this.id = UUID.randomUUID().toString();
        this.countNumber = countNumber;
        this.warehouseId = warehouseId;
        this.countDate = countDate;
        this.status = Status.DRAFT;
    }

    public void start() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT cycle counts can be started");
        }
        this.status = Status.IN_PROGRESS;
    }

    public void submit() {
        if (this.status != Status.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS cycle counts can be submitted");
        }
        this.status = Status.SUBMITTED;
    }

    public void adjust() {
        if (this.status != Status.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED cycle counts can be adjusted");
        }
        this.status = Status.ADJUSTED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getCountNumber() { return countNumber; }
    public String getWarehouseId() { return warehouseId; }
    public LocalDate getCountDate() { return countDate; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
