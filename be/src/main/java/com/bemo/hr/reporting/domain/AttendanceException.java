package com.bemo.hr.reporting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_exceptions")
@Getter
public class AttendanceException {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "report_id", nullable = false, length = 36)
    private String reportId;
    @Column(name = "daily_result_id", nullable = false, length = 36)
    private String dailyResultId;
    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;
    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 30)
    private AttendanceExceptionType exceptionType;
    @Column(nullable = false)
    private int score;
    @Column(name = "explanation_key", nullable = false, length = 100)
    private String explanationKey;
    @Column(name = "policy_id", length = 36)
    private String policyId;
    @Column(name = "policy_name", nullable = false, length = 120)
    private String policyName;
    @Column(name = "policy_version", nullable = false)
    private long policyVersion;
    @Column(name = "policy_snapshot_json", nullable = false, length = 1000)
    private String policySnapshotJson;
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_scope", nullable = false, length = 20)
    private AttendancePolicyScope policyScope;
    @Column(name = "payroll_blocking", nullable = false)
    private boolean payrollBlocking;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceExceptionStatus status;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AttendanceExceptionResolution resolution;
    @Column(length = 500)
    private String reason;
    @Column(name = "operation_id", length = 80)
    private String operationId;
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceException() {
    }

    public AttendanceException(DailyAttendanceResult result, AttendanceExceptionType type, int score, String explanation,
                               AttendancePolicy policy, boolean blocking) {
        id = UUID.randomUUID().toString();
        reportId = result.getReportId();
        dailyResultId = result.getId();
        employeeId = result.getEmployeeId();
        categoryId = result.getCategoryId();
        workDate = result.getWorkDate();
        exceptionType = type;
        this.score = score;
        explanationKey = explanation;
        policyId = policy == null ? null : policy.getId();
        policyName = policy == null ? "Default" : policy.getName();
        policyVersion = policy == null ? 0 : policy.getVersion();
        policyScope = policy == null ? AttendancePolicyScope.TENANT : policy.getScopeType();
        policySnapshotJson = policy == null ? "{}" : "{\"lateThresholdMinutes\":" + policy.getLateThresholdMinutes()
                                                     + ",\"earlyThresholdMinutes\":" + policy.getEarlyThresholdMinutes() + ",\"maxShiftMinutes\":" + policy.getMaxShiftMinutes()
                                                     + ",\"payrollBlockScore\":" + policy.getPayrollBlockScore() + "}";
        payrollBlocking = blocking;
        status = AttendanceExceptionStatus.OPEN;
    }

    public boolean replay(String operation) {
        return operationId != null && operationId.equals(operation);
    }

    public void resolve(AttendanceExceptionResolution resolution, String reason, String operation, String actor) {
        if (status != AttendanceExceptionStatus.OPEN) throw new IllegalStateException("Attendance exception is closed");
        this.resolution = resolution;
        this.reason = reason.strip();
        operationId = operation;
        resolvedBy = actor;
        resolvedAt = Instant.now();
        status = resolution == AttendanceExceptionResolution.IGNORE ? AttendanceExceptionStatus.IGNORED :
                resolution == AttendanceExceptionResolution.ACCEPT ? AttendanceExceptionStatus.RESOLVED : AttendanceExceptionStatus.OVERRIDDEN;
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
