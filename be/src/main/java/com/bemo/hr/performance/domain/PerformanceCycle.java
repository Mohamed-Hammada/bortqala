package com.bemo.hr.performance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "performance_cycles")
public class PerformanceCycle {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "name_ar", nullable = false, length = 200)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CycleStatus status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PerformanceCycle() {
    }

    public PerformanceCycle(String nameAr, String nameEn, int periodYear, LocalDate startDate, LocalDate endDate) {
        this.id = UUID.randomUUID().toString();
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.periodYear = periodYear;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = CycleStatus.ACTIVE;
    }

    public void lock() {
        this.status = CycleStatus.LOCKED;
    }

    public void close() {
        this.status = CycleStatus.CLOSED;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public CycleStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
