package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "material_reservation_headers")
public class MaterialReservationHeader {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected MaterialReservationHeader() {
    }

    public MaterialReservationHeader(String workOrderId) {
        this.id = UUID.randomUUID().toString();
        this.workOrderId = workOrderId;
        this.status = Status.ACTIVE;
    }

    public void release() {
        this.status = Status.RELEASED;
    }

    public void cancel() {
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

    public String getWorkOrderId() {
        return workOrderId;
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
        ACTIVE, RELEASED, CANCELLED
    }
}
