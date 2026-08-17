package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worker_settlements")
@Getter
public class WorkerSettlement {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;
    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;
    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;
    @Column(name = "total_attendance_units", precision = 8, scale = 2)
    private BigDecimal totalAttendanceUnits;
    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate;
    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount;
    @Column(name = "overtime_amount", precision = 12, scale = 2)
    private BigDecimal overtimeAmount;
    @Column(name = "deductions_amount", precision = 12, scale = 2)
    private BigDecimal deductionsAmount;
    @Column(name = "advance_deductions", precision = 12, scale = 2)
    private BigDecimal advanceDeductions;
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;
    @Column(name = "advance_policy_snapshot", length = 2000)
    private String advancePolicySnapshot;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkerSettlement() {
    }

    public WorkerSettlement(String periodId, String workerId, String contractorId,
                            BigDecimal totalAttendanceUnits, BigDecimal dailyRate,
                            BigDecimal grossAmount, BigDecimal overtimeAmount,
                            BigDecimal deductionsAmount, BigDecimal advanceDeductions,
                            BigDecimal netAmount) {
        this.id = UUID.randomUUID().toString();
        this.periodId = periodId;
        this.workerId = workerId;
        this.contractorId = contractorId;
        this.totalAttendanceUnits = totalAttendanceUnits != null ? totalAttendanceUnits : BigDecimal.ZERO;
        this.dailyRate = dailyRate != null ? dailyRate : BigDecimal.ZERO;
        this.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        this.overtimeAmount = overtimeAmount != null ? overtimeAmount : BigDecimal.ZERO;
        this.deductionsAmount = deductionsAmount != null ? deductionsAmount : BigDecimal.ZERO;
        this.advanceDeductions = advanceDeductions != null ? advanceDeductions : BigDecimal.ZERO;
        this.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
    }

    public void applyAdvancePolicySnapshot(String snapshot) {
        this.advancePolicySnapshot = snapshot;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
