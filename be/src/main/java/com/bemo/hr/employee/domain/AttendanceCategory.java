package com.bemo.hr.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "attendance_categories")
public class AttendanceCategory {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "expected_daily_minutes", nullable = false)
    private int expectedDailyMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_cycle", nullable = false, length = 20)
    private PayCycle payCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_mode", nullable = false, length = 20)
    private AttendanceMode attendanceMode;

    @Column(name = "single_punch_counts", nullable = false)
    private boolean singlePunchCounts;

    @Column(name = "allows_employee_advances", nullable = false)
    private boolean allowsEmployeeAdvances;

    @Column(name = "work_days_mask", nullable = false)
    private int workDaysMask;

    @Column(nullable = false)
    private boolean active;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceCategory() {
    }

    public AttendanceCategory(String code, String name, int expectedDailyMinutes, PayCycle payCycle, AttendanceMode attendanceMode,
                              boolean singlePunchCounts, int workDaysMask, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, expectedDailyMinutes, payCycle, attendanceMode, singlePunchCounts, workDaysMask, active);
    }

    public void configureAdvanceEligibility(boolean allowed) {
        this.allowsEmployeeAdvances = allowed;
    }

    public void update(String code, String name, int expectedDailyMinutes, PayCycle payCycle, AttendanceMode attendanceMode,
                       boolean singlePunchCounts, int workDaysMask, boolean active) {
        this.code = code.strip().toUpperCase(Locale.ROOT);
        this.name = name.strip();
        this.expectedDailyMinutes = expectedDailyMinutes;
        this.payCycle = payCycle;
        this.attendanceMode = attendanceMode;
        this.singlePunchCounts = singlePunchCounts;
        this.workDaysMask = workDaysMask;
        this.active = active;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getExpectedDailyMinutes() { return expectedDailyMinutes; }
    public PayCycle getPayCycle() { return payCycle; }
    public AttendanceMode getAttendanceMode() { return attendanceMode; }
    public boolean isSinglePunchCounts() { return singlePunchCounts; }
    public boolean isAllowsEmployeeAdvances() { return allowsEmployeeAdvances; }
    public int getWorkDaysMask() { return workDaysMask; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
