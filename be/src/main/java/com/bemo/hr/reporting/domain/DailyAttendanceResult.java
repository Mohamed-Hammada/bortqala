package com.bemo.hr.reporting.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_results")
public class DailyAttendanceResult {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "report_id", nullable = false)
    private String reportId;
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    @Column(name = "category_id", nullable = false)
    private String categoryId;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Column(name = "employee_code", nullable = false)
    private String employeeCode;
    @Column(name = "employee_name", nullable = false)
    private String employeeName;
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    @Column(name = "first_punch")
    private Instant firstPunch;
    @Column(name = "last_punch")
    private Instant lastPunch;
    @Column(name = "punch_count", nullable = false)
    private int punchCount;
    @Column(name = "expected_minutes", nullable = false)
    private int expectedMinutes;
    @Column(name = "worked_minutes", nullable = false)
    private int workedMinutes;
    @Column(name = "manual_worked_minutes")
    private Integer manualWorkedMinutes;
    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes;
    @Column(name = "early_leave_minutes", nullable = false)
    private int earlyLeaveMinutes;
    @Column(name = "overtime_minutes", nullable = false)
    private int overtimeMinutes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DailyStatus status;
    @Column(length = 255)
    private String warning;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AttendanceDecision decision;
    @Column(name = "decision_note", length = 500)
    private String decisionNote;
    @Column(name = "decided_by", length = 100)
    private String decidedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "rule_version", nullable = false, length = 100)
    private String ruleVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected DailyAttendanceResult() {
    }

    public DailyAttendanceResult(String reportId, String employeeId, String categoryId, LocalDate workDate,
                                 String employeeCode, String employeeName, String categoryName,
                                 Instant firstPunch, Instant lastPunch, int punchCount, int expectedMinutes,
                                 int workedMinutes, int lateMinutes, int earlyLeaveMinutes, int overtimeMinutes,
                                 DailyStatus status, String warning, String ruleVersion) {
        this.id = UUID.randomUUID().toString();
        this.reportId = reportId;
        this.employeeId = employeeId;
        this.categoryId = categoryId;
        this.workDate = workDate;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.categoryName = categoryName;
        this.firstPunch = firstPunch;
        this.lastPunch = lastPunch;
        this.punchCount = punchCount;
        this.expectedMinutes = expectedMinutes;
        this.workedMinutes = workedMinutes;
        this.lateMinutes = lateMinutes;
        this.earlyLeaveMinutes = earlyLeaveMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.status = status;
        this.warning = warning;
        this.ruleVersion = ruleVersion;
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

    public boolean isBlocking() {
        return decision == null && (status == DailyStatus.NO_PUNCH || status == DailyStatus.SINGLE_PUNCH || status == DailyStatus.MANUAL_ENTRY || status == DailyStatus.MISSING_SCHEDULE);
    }

    public DecisionState decisionState() {
        return new DecisionState(decision, manualWorkedMinutes, decisionNote, decidedBy, decidedAt);
    }

    public void decide(AttendanceDecision decision, Integer manualWorkedMinutes, String note, String actor) {
        this.decision = decision;
        this.manualWorkedMinutes = manualWorkedMinutes;
        this.decisionNote = note;
        this.decidedBy = actor;
        this.decidedAt = Instant.now();
    }

    public void restoreDecision(AttendanceDecision decision, Integer manualWorkedMinutes, String note,
                                String actor, Instant decidedAt) {
        this.decision = decision;
        this.manualWorkedMinutes = manualWorkedMinutes;
        this.decisionNote = note;
        this.decidedBy = actor;
        this.decidedAt = decidedAt;
    }

    public void confirmHoliday(String actor) {
        status = DailyStatus.HOLIDAY;
        warning = null;
        decide(AttendanceDecision.APPROVED_LEAVE, null, "Confirmed category holiday", actor);
    }

    public String getId() {
        return id;
    }

    public String getReportId() {
        return reportId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Instant getFirstPunch() {
        return firstPunch;
    }

    public Instant getLastPunch() {
        return lastPunch;
    }

    public int getPunchCount() {
        return punchCount;
    }

    public int getExpectedMinutes() {
        return expectedMinutes;
    }

    public int getWorkedMinutes() {
        return workedMinutes;
    }

    public Integer getManualWorkedMinutes() {
        return manualWorkedMinutes;
    }

    public int getEffectiveWorkedMinutes() {
        return manualWorkedMinutes == null ? workedMinutes : manualWorkedMinutes;
    }

    public int getLateMinutes() {
        return lateMinutes;
    }

    public int getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public DailyStatus getStatus() {
        return status;
    }

    public String getWarning() {
        return warning;
    }

    public AttendanceDecision getDecision() {
        return decision;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public long getVersion() {
        return version;
    }

    public record DecisionState(AttendanceDecision decision, Integer manualWorkedMinutes, String note,
                                String decidedBy, Instant decidedAt) {
    }
}
