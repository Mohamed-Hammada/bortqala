package com.bemo.hr.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_rules")
public class ScheduleRule {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "expected_minutes_override")
    private Integer expectedMinutesOverride;

    @Column(name = "grace_minutes", nullable = false)
    private int graceMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduleRule() {
    }

    public ScheduleRule(String categoryId, String name, LocalDate effectiveFrom, LocalDate effectiveTo,
                        LocalTime startTime, Integer expectedMinutesOverride, int graceMinutes) {
        this.id = UUID.randomUUID().toString();
        this.categoryId = categoryId;
        this.name = name.strip();
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.startTime = startTime;
        this.expectedMinutesOverride = expectedMinutesOverride;
        this.graceMinutes = graceMinutes;
    }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public boolean appliesOn(LocalDate date) {
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public LocalTime getStartTime() { return startTime; }
    public Integer getExpectedMinutesOverride() { return expectedMinutesOverride; }
    public int getGraceMinutes() { return graceMinutes; }
}
