package com.bemo.hr.reporting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_policies")
@Getter
public class AttendancePolicy {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private AttendancePolicyScope scopeType;
    @Column(name = "scope_id", length = 36)
    private String scopeId;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Column(nullable = false)
    private int priority;
    @Column(name = "late_threshold_minutes", nullable = false)
    private int lateThresholdMinutes;
    @Column(name = "early_threshold_minutes", nullable = false)
    private int earlyThresholdMinutes;
    @Column(name = "max_shift_minutes", nullable = false)
    private int maxShiftMinutes;
    @Column(name = "missing_punch_score", nullable = false)
    private int missingPunchScore;
    @Column(name = "single_punch_score", nullable = false)
    private int singlePunchScore;
    @Column(name = "late_score", nullable = false)
    private int lateScore;
    @Column(name = "early_score", nullable = false)
    private int earlyScore;
    @Column(name = "payroll_block_score", nullable = false)
    private int payrollBlockScore;
    @Column(nullable = false)
    private boolean active;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendancePolicy() {
    }

    public AttendancePolicy(String name, AttendancePolicyScope scopeType, String scopeId, LocalDate from, LocalDate to,
                            int priority, int lateThreshold, int earlyThreshold, int maxShift, int missingScore, int singleScore,
                            int lateScore, int earlyScore, int payrollBlockScore, boolean active) {
        id = UUID.randomUUID().toString();
        this.name = name.strip();
        this.scopeType = scopeType;
        this.scopeId = scopeType == AttendancePolicyScope.TENANT ? null : scopeId.strip();
        effectiveFrom = from;
        effectiveTo = to;
        this.priority = priority;
        lateThresholdMinutes = lateThreshold;
        earlyThresholdMinutes = earlyThreshold;
        maxShiftMinutes = maxShift;
        missingPunchScore = missingScore;
        singlePunchScore = singleScore;
        this.lateScore = lateScore;
        this.earlyScore = earlyScore;
        this.payrollBlockScore = payrollBlockScore;
        this.active = active;
    }

    public static AttendancePolicy defaultPolicy() {
        AttendancePolicy policy = new AttendancePolicy("Default", AttendancePolicyScope.TENANT, null,
                LocalDate.of(1970, 1, 1), null, 0, 15, 15, 960, 100, 70, 30, 30, 70, true);
        policy.id = null;
        return policy;
    }

    public boolean applies(String employeeId, String categoryId, LocalDate date) {
        if (!active || date.isBefore(effectiveFrom) || effectiveTo != null && date.isAfter(effectiveTo)) return false;
        return scopeType == AttendancePolicyScope.TENANT || scopeType == AttendancePolicyScope.CATEGORY && scopeId.equals(categoryId)
                || scopeType == AttendancePolicyScope.EMPLOYEE && scopeId.equals(employeeId);
    }

    public int specificity() {
        return scopeType == AttendancePolicyScope.EMPLOYEE ? 3 : scopeType == AttendancePolicyScope.CATEGORY ? 2 : 1;
    }

    @PrePersist
    void create() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }
}
