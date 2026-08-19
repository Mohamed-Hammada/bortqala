package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_schedules")
public class ProjectSchedule {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "calendar_code", length = 50, nullable = false)
    private String calendarCode;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ScheduleStatus status;

    @Column(name = "current_baseline_version", nullable = false)
    private int currentBaselineVersion;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectSchedule() {
    }

    public ProjectSchedule(String projectId, String name, String calendarCode, LocalDate startDate, LocalDate endDate) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.name = name != null ? name.strip() : "Project Schedule";
        this.calendarCode = calendarCode != null ? calendarCode.strip() : "STANDARD_6DAY";
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ScheduleStatus.DRAFT;
        this.currentBaselineVersion = 0;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String calendarCode, LocalDate startDate, LocalDate endDate, ScheduleStatus status) {
        if (name != null && !name.isBlank()) {
            this.name = name.strip();
        }
        if (calendarCode != null && !calendarCode.isBlank()) {
            this.calendarCode = calendarCode.strip();
        }
        this.startDate = startDate;
        this.endDate = endDate;
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public void incrementBaselineVersion() {
        this.currentBaselineVersion++;
        this.status = ScheduleStatus.BASELINE_LOCKED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = System.currentTimeMillis();
    }
}
