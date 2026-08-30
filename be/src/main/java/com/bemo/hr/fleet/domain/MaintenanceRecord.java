package com.bemo.hr.fleet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "flt_maintenance_records")
public class MaintenanceRecord {


    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    @Column(name = "schedule_id", length = 64)
    private String scheduleId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "performed_date", length = 32, nullable = false)
    private String performedDate;

    @Column(name = "odometer", precision = 15, scale = 2, nullable = false)
    private BigDecimal odometer;

    @Column(name = "cost", precision = 15, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(name = "vendor_party_id", length = 64)
    private String vendorPartyId;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(String appId, String vehicleId, String scheduleId, String title,
                             String performedDate, BigDecimal odometer, BigDecimal cost,
                             String vendorPartyId, String vendorName, String description) {
        this.id = "MREC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.appId = appId;
        this.vehicleId = vehicleId;
        this.scheduleId = scheduleId;
        this.title = title;
        this.performedDate = performedDate;
        this.odometer = odometer;
        this.cost = cost;
        this.vendorPartyId = vendorPartyId;
        this.vendorName = vendorName;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAppId() {
        return appId;
    }


    public String getId() {
        return id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public String getTitle() {
        return title;
    }

    public String getPerformedDate() {
        return performedDate;
    }

    public BigDecimal getOdometer() {
        return odometer;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getVendorPartyId() {
        return vendorPartyId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getDescription() {
        return description;
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
}
