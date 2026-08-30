package com.bemo.hr.fleet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "flt_maintenance_schedules")
public class MaintenanceSchedule {


    public enum MaintenanceKind {
        OIL, TIRES, INSPECTION, BRAKES, CUSTOM
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_kind", length = 32, nullable = false)
    private MaintenanceKind maintenanceKind;

    @Column(name = "interval_km", precision = 15, scale = 2)
    private BigDecimal intervalKm;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "last_done_odometer", precision = 15, scale = 2)
    private BigDecimal lastDoneOdometer;

    @Column(name = "last_done_date", length = 32)
    private String lastDoneDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected MaintenanceSchedule() {
    }

    public MaintenanceSchedule(String appId, String vehicleId, String title, MaintenanceKind maintenanceKind,
                               BigDecimal intervalKm, Integer intervalDays, BigDecimal lastDoneOdometer, String lastDoneDate) {
        this.id = "SCHED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.appId = appId;
        this.vehicleId = vehicleId;
        this.title = title;
        this.maintenanceKind = maintenanceKind != null ? maintenanceKind : MaintenanceKind.OIL;
        this.intervalKm = intervalKm;
        this.intervalDays = intervalDays;
        this.lastDoneOdometer = lastDoneOdometer;
        this.lastDoneDate = lastDoneDate;
        this.active = true;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }

    public MaintenanceKind getMaintenanceKind() {
        return maintenanceKind;
    }

    public void setMaintenanceKind(MaintenanceKind maintenanceKind) {
        this.maintenanceKind = maintenanceKind;
        this.updatedAt = System.currentTimeMillis();
    }

    public BigDecimal getIntervalKm() {
        return intervalKm;
    }

    public void setIntervalKm(BigDecimal intervalKm) {
        this.intervalKm = intervalKm;
        this.updatedAt = System.currentTimeMillis();
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
        this.updatedAt = System.currentTimeMillis();
    }

    public BigDecimal getLastDoneOdometer() {
        return lastDoneOdometer;
    }

    public void setLastDoneOdometer(BigDecimal lastDoneOdometer) {
        this.lastDoneOdometer = lastDoneOdometer;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getLastDoneDate() {
        return lastDoneDate;
    }

    public void setLastDoneDate(String lastDoneDate) {
        this.lastDoneDate = lastDoneDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
