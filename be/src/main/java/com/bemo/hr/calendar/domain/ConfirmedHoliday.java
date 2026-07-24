package com.bemo.hr.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "confirmed_holidays")
public class ConfirmedHoliday {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "category_id", nullable = false)
    private String categoryId;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "confirmed_by", nullable = false, length = 100)
    private String confirmedBy;
    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    protected ConfirmedHoliday() {
    }

    public ConfirmedHoliday(String categoryId, LocalDate workDate, String name, String confirmedBy) {
        this.id = UUID.randomUUID().toString();
        this.categoryId = categoryId;
        this.workDate = workDate;
        this.name = name.strip();
        this.confirmedBy = confirmedBy;
    }

    @PrePersist
    void prePersist() { confirmedAt = Instant.now(); }

    public String getCategoryId() { return categoryId; }
    public LocalDate getWorkDate() { return workDate; }
}
