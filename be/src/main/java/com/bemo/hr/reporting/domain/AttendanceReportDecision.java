package com.bemo.hr.reporting.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_report_decisions")
public class AttendanceReportDecision {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "report_id", nullable = false)
    private String reportId;
    @Column(name = "result_id", nullable = false)
    private String resultId;
    @Column(name = "operation_id", nullable = false, length = 64)
    private String operationId;
    @Column(nullable = false, length = 30)
    private String operation;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_decision", length = 30)
    private AttendanceDecision previousDecision;
    @Column(name = "previous_manual_minutes")
    private Integer previousManualMinutes;
    @Column(name = "previous_note", length = 500)
    private String previousNote;
    @Column(name = "previous_decided_by", length = 100)
    private String previousDecidedBy;
    @Column(name = "previous_decided_at")
    private Instant previousDecidedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_decision", nullable = false, length = 30)
    private AttendanceDecision newDecision;
    @Column(name = "new_manual_minutes")
    private Integer newManualMinutes;
    @Column(name = "new_note", length = 500)
    private String newNote;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AttendanceReportDecision() {
    }

    public AttendanceReportDecision(String reportId, String resultId, String operationId, String operation,
                                    DailyAttendanceResult.DecisionState previous,
                                    DailyAttendanceResult.DecisionState next, String actor) {
        this.id = UUID.randomUUID().toString();
        this.reportId = reportId;
        this.resultId = resultId;
        this.operationId = operationId;
        this.operation = operation;
        this.previousDecision = previous.decision();
        this.previousManualMinutes = previous.manualWorkedMinutes();
        this.previousNote = previous.note();
        this.previousDecidedBy = previous.decidedBy();
        this.previousDecidedAt = previous.decidedAt();
        this.newDecision = next.decision();
        this.newManualMinutes = next.manualWorkedMinutes();
        this.newNote = next.note();
        this.actor = actor;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getReportId() {
        return reportId;
    }

    public String getResultId() {
        return resultId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperation() {
        return operation;
    }

    public AttendanceDecision getPreviousDecision() {
        return previousDecision;
    }

    public Integer getPreviousManualMinutes() {
        return previousManualMinutes;
    }

    public String getPreviousNote() {
        return previousNote;
    }

    public String getPreviousDecidedBy() {
        return previousDecidedBy;
    }

    public Instant getPreviousDecidedAt() {
        return previousDecidedAt;
    }

    public AttendanceDecision getNewDecision() {
        return newDecision;
    }

    public Integer getNewManualMinutes() {
        return newManualMinutes;
    }

    public String getNewNote() {
        return newNote;
    }

    public String getActor() {
        return actor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
