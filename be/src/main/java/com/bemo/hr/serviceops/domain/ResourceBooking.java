package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "srv_resource_bookings")
@Getter
@Setter
public class ResourceBooking {

    public enum Status {
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "customer_party_id", length = 36)
    private String customerPartyId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "start_time", nullable = false)
    private long startTime;

    @Column(name = "end_time", nullable = false)
    private long endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(length = 1000)
    private String notes;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected ResourceBooking() {}

    public ResourceBooking(String appId, String resourceId, String title,
                           String customerPartyId, String customerName,
                           long startTime, long endTime, String notes) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.resourceId = resourceId;
        this.title = title;
        this.customerPartyId = customerPartyId;
        this.customerName = customerName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.CONFIRMED;
        this.notes = notes;
        this.version = 0L;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
