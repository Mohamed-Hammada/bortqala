package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "work_centers")
public class WorkCenter {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "hourly_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "capacity_hours_per_day", nullable = false, precision = 5, scale = 2)
    private BigDecimal capacityHoursPerDay;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkCenter() {}

    public WorkCenter(String code, String name, BigDecimal hourlyRate, BigDecimal capacityHoursPerDay) {
        this.id = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.hourlyRate = hourlyRate;
        this.capacityHoursPerDay = capacityHoursPerDay;
        this.active = true;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public BigDecimal getCapacityHoursPerDay() { return capacityHoursPerDay; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
