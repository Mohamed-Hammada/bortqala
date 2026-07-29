package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "manual_attendance_entries")
@Getter
public class ManualAttendanceEntry {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "worker_id", nullable = false, length = 36) private String workerId;
    @Column(name = "work_date", nullable = false, length = 10) private String workDate;
    @Column(name = "attendance_value", precision = 4, scale = 2, nullable = false) private BigDecimal attendanceValue;
    @Column(name = "check_in", length = 10) private String checkIn;
    @Column(name = "check_out", length = 10) private String checkOut;
    @Column(name = "actual_hours", precision = 4, scale = 2) private BigDecimal actualHours;
    @Column(name = "overtime_hours", precision = 4, scale = 2) private BigDecimal overtimeHours;
    @Column(name = "deduction_hours", precision = 4, scale = 2) private BigDecimal deductionHours;
    @Column(name = "effective_daily_rate", precision = 12, scale = 2) private BigDecimal effectiveDailyRate;
    @Column(nullable = false, length = 30) private String source;
    @Column(length = 500) private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ManualAttendanceEntry() { }

    public ManualAttendanceEntry(String workerId, String workDate, BigDecimal attendanceValue,
                                 String checkIn, String checkOut, BigDecimal actualHours,
                                 BigDecimal overtimeHours, BigDecimal deductionHours,
                                 BigDecimal effectiveDailyRate, String source, String notes) {
        this.id = UUID.randomUUID().toString();
        update(workerId, workDate, attendanceValue, checkIn, checkOut, actualHours, overtimeHours, deductionHours, effectiveDailyRate, source, notes);
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
                && Objects.equals(normalizeText(this.notes), normalizeText(notes));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean decimalEquals(BigDecimal left, BigDecimal right) {
        return zeroIfNull(left).compareTo(zeroIfNull(right)) == 0;
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
