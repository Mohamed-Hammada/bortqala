package com.bemo.hr.fleet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "flt_fuel_logs")
public class FuelLog {


    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    @Column(name = "log_date", length = 32, nullable = false)
    private String logDate;

    @Column(name = "liters", precision = 10, scale = 2, nullable = false)
    private BigDecimal liters;

    @Column(name = "odometer", precision = 15, scale = 2, nullable = false)
    private BigDecimal odometer;

    @Column(name = "total_cost", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Column(name = "station_name", length = 255)
    private String stationName;

    @Column(name = "driver_name", length = 255)
    private String driverName;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected FuelLog() {
    }

    public FuelLog(String appId, String vehicleId, String logDate, BigDecimal liters, BigDecimal odometer,
                   BigDecimal totalCost, String stationName, String driverName, String notes) {
        this.id = "FUEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.appId = appId;
        this.vehicleId = vehicleId;
        this.logDate = logDate;
        this.liters = liters;
        this.odometer = odometer;
        this.totalCost = totalCost;
        this.stationName = stationName;
        this.driverName = driverName;
        this.notes = notes;
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

    public String getLogDate() {
        return logDate;
    }

    public BigDecimal getLiters() {
        return liters;
    }

    public BigDecimal getOdometer() {
        return odometer;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public String getStationName() {
        return stationName;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getNotes() {
        return notes;
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
