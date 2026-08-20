package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "manual_attendance_entries")
@Getter
public class ManualAttendanceEntry {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;
    @Column(name = "project_id", length = 36)
    private String projectId;
    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;
    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;
    @Column(name = "work_date", nullable = false, length = 10)
    private String workDate;
    @Column(name = "attendance_value", precision = 4, scale = 2, nullable = false)
    private BigDecimal attendanceValue;
    @Column(name = "check_in", length = 10)
    private String checkIn;
    @Column(name = "check_out", length = 10)
    private String checkOut;
    @Column(name = "actual_hours", precision = 4, scale = 2)
    private BigDecimal actualHours;
    @Column(name = "overtime_hours", precision = 4, scale = 2)
    private BigDecimal overtimeHours;
    @Column(name = "deduction_hours", precision = 4, scale = 2)
    private BigDecimal deductionHours;
    @Column(name = "effective_daily_rate", precision = 12, scale = 2)
    private BigDecimal effectiveDailyRate;
    @Column(nullable = false, length = 30)
    private String source;
    @Column(length = 500)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ManualAttendanceEntry() {
    }

    public ManualAttendanceEntry(String workerId, String workDate, BigDecimal attendanceValue,
                                 String checkIn, String checkOut, BigDecimal actualHours,
                                 BigDecimal overtimeHours, BigDecimal deductionHours,
                                 BigDecimal effectiveDailyRate, String source, String notes) {
        this(workerId, null, null, null, workDate, attendanceValue, checkIn, checkOut, actualHours, overtimeHours, deductionHours, effectiveDailyRate, source, notes);
    }

    public ManualAttendanceEntry(String workerId, String projectId, String wbsNodeId, String costCodeId,
                                 String workDate, BigDecimal attendanceValue,
                                 String checkIn, String checkOut, BigDecimal actualHours,
                                 BigDecimal overtimeHours, BigDecimal deductionHours,
                                 BigDecimal effectiveDailyRate, String source, String notes) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        update(workerId, workDate, attendanceValue, checkIn, checkOut, actualHours, overtimeHours, deductionHours, effectiveDailyRate, source, notes);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean decimalEquals(BigDecimal left, BigDecimal right) {
        return zeroIfNull(left).compareTo(zeroIfNull(right)) == 0;
    }

    public void update(String workerId, String workDate, BigDecimal attendanceValue,
                       String checkIn, String checkOut, BigDecimal actualHours,
                       BigDecimal overtimeHours, BigDecimal deductionHours,
                       BigDecimal effectiveDailyRate, String source, String notes) {
        this.workerId = workerId;
        this.workDate = workDate;
        this.attendanceValue = attendanceValue != null ? attendanceValue : BigDecimal.ONE;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.actualHours = actualHours != null ? actualHours : BigDecimal.ZERO;
        this.overtimeHours = overtimeHours != null ? overtimeHours : BigDecimal.ZERO;
        this.deductionHours = deductionHours != null ? deductionHours : BigDecimal.ZERO;
        this.effectiveDailyRate = effectiveDailyRate != null ? effectiveDailyRate : BigDecimal.ZERO;
        this.source = source != null ? source.strip().toUpperCase() : "MANUAL";
        this.notes = notes;
    }

    public void assignProject(String projectId, String wbsNodeId, String costCodeId) {
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
    }

    public boolean hasSameManualValues(BigDecimal attendanceValue, String checkIn, String checkOut,
                                       BigDecimal actualHours, BigDecimal overtimeHours,
                                       BigDecimal deductionHours, BigDecimal effectiveDailyRate,
                                       String notes) {
        return decimalEquals(this.attendanceValue, attendanceValue)
                && Objects.equals(this.checkIn, checkIn)
                && Objects.equals(this.checkOut, checkOut)
                && decimalEquals(this.actualHours, zeroIfNull(actualHours))
                && decimalEquals(this.overtimeHours, zeroIfNull(overtimeHours))
                && decimalEquals(this.deductionHours, zeroIfNull(deductionHours))
                && decimalEquals(this.effectiveDailyRate, zeroIfNull(effectiveDailyRate))
                && Objects.equals(this.notes, notes);
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
}
