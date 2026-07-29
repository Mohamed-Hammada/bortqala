package com.bemo.hr.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_day_anomaly_results")
public class DayAnomalyResultSnapshot {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "anomaly_id", nullable = false) private String anomalyId;
    @Column(name = "daily_result_id", nullable = false) private String dailyResultId;
    @Enumerated(EnumType.STRING) @Column(name = "previous_decision", length = 30) private AttendanceDecision previousDecision;
    @Column(name = "previous_manual_minutes") private Integer previousManualMinutes;
    @Column(name = "previous_note", length = 500) private String previousNote;
    @Column(name = "previous_decided_by", length = 160) private String previousDecidedBy;
    @Column(name = "previous_decided_at") private Instant previousDecidedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected DayAnomalyResultSnapshot() { }

    public DayAnomalyResultSnapshot(String anomalyId, DailyAttendanceResult result) {
        this.id = UUID.randomUUID().toString();
        this.anomalyId = anomalyId;
        this.dailyResultId = result.getId();
        this.previousDecision = result.getDecision();
        this.previousManualMinutes = result.getManualWorkedMinutes();
        this.previousNote = result.getDecisionNote();
        this.previousDecidedBy = result.getDecidedBy();
        this.previousDecidedAt = result.getDecidedAt();
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); }
    public String getAnomalyId() { return anomalyId; }
    public String getDailyResultId() { return dailyResultId; }
    public AttendanceDecision getPreviousDecision() { return previousDecision; }
    public Integer getPreviousManualMinutes() { return previousManualMinutes; }
    public String getPreviousNote() { return previousNote; }
    public String getPreviousDecidedBy() { return previousDecidedBy; }
    public Instant getPreviousDecidedAt() { return previousDecidedAt; }
}
