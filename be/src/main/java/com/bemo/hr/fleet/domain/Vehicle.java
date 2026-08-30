package com.bemo.hr.fleet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "flt_vehicles")
public class Vehicle {


    public enum VehicleType {
        SEDAN, TRUCK, VAN, BUS, HEAVY_EQUIPMENT
    }

    public enum Status {
        ACTIVE, MAINTENANCE, RETIRED
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "plate_number", length = 64, nullable = false)
    private String plateNumber;

    @Column(name = "make", length = 128, nullable = false)
    private String make;

    @Column(name = "model", length = 128, nullable = false)
    private String model;

    @Column(name = "year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 32, nullable = false)
    private VehicleType vehicleType;

    @Column(name = "asset_id", length = 64)
    private String assetId;

    @Column(name = "default_driver_id", length = 64)
    private String defaultDriverId;

    @Column(name = "default_driver_name", length = 255)
    private String defaultDriverName;

    @Column(name = "current_odometer", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentOdometer = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Vehicle() {
    }

    public Vehicle(String appId, String plateNumber, String make, String model, int year, VehicleType vehicleType,
                   String assetId, String defaultDriverId, String defaultDriverName, BigDecimal initialOdometer, String notes) {
        this.id = "VEH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.appId = appId;
        this.plateNumber = plateNumber;
        this.make = make;
        this.model = model;
        this.year = year;
        this.vehicleType = vehicleType != null ? vehicleType : VehicleType.SEDAN;
        this.assetId = assetId;
        this.defaultDriverId = defaultDriverId;
        this.defaultDriverName = defaultDriverName;
        this.currentOdometer = initialOdometer != null ? initialOdometer : BigDecimal.ZERO;
        this.status = Status.ACTIVE;
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

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
        this.updatedAt = System.currentTimeMillis();
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDefaultDriverId() {
        return defaultDriverId;
    }

    public void setDefaultDriverId(String defaultDriverId) {
        this.defaultDriverId = defaultDriverId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDefaultDriverName() {
        return defaultDriverName;
    }

    public void setDefaultDriverName(String defaultDriverName) {
        this.defaultDriverName = defaultDriverName;
        this.updatedAt = System.currentTimeMillis();
    }

    public BigDecimal getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(BigDecimal currentOdometer) {
        this.currentOdometer = currentOdometer;
        this.updatedAt = System.currentTimeMillis();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
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
