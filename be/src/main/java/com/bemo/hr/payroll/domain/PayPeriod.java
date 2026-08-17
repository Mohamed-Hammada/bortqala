package com.bemo.hr.payroll.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pay_periods")
public class PayPeriod {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "calendar_id", nullable = false, length = 36)
    private String calendarId;
    @Column(name = "period_number", nullable = false)
    private int periodNumber;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PayPeriod() {
    }

    public PayPeriod(String calendarId, int periodNumber, LocalDate startDate, LocalDate endDate) {
        this.id = UUID.randomUUID().toString();
        this.calendarId = calendarId;
        this.periodNumber = periodNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = Status.OPEN;
    }

    public void process() {
        if (this.status != Status.OPEN) {
            throw new IllegalStateException("Only OPEN pay periods can be processed");
        }
        this.status = Status.PROCESSING;
    }

    public void close() {
        if (this.status != Status.PROCESSING && this.status != Status.OPEN) {
            throw new IllegalStateException("Pay period is already CLOSED");
        }
        this.status = Status.CLOSED;
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

    public String getCalendarId() {
        return calendarId;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
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
        OPEN, PROCESSING, CLOSED
    }
}
