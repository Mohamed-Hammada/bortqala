package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contractor_settlement_lines")
@Getter
public class ContractorSettlementLine {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "settlement_id", nullable = false, length = 36)
    private String settlementId;
    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;
    @Column(name = "project_id", length = 36)
    private String projectId;
    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;
    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;
    @Column(name = "attendance_days", precision = 8, scale = 2)
    private BigDecimal attendanceDays;
    @Column(name = "daily_wage", precision = 12, scale = 2)
    private BigDecimal dailyWage;
    @Column(name = "gross_wage", precision = 12, scale = 2)
    private BigDecimal grossWage;
    @Column(name = "overtime_amount", precision = 12, scale = 2)
    private BigDecimal overtimeAmount;
    @Column(name = "deductions_amount", precision = 12, scale = 2)
    private BigDecimal deductionsAmount;
    @Column(name = "advance_installments", precision = 12, scale = 2)
    private BigDecimal advanceInstallments;
    @Column(name = "net_wage", precision = 12, scale = 2)
    private BigDecimal netWage;
    @Column(name = "calculation_details_json", length = 2000)
    private String calculationDetailsJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ContractorSettlementLine() {
    }

    public ContractorSettlementLine(String settlementId, String workerId, BigDecimal attendanceDays,
                                    BigDecimal dailyWage, BigDecimal grossWage, BigDecimal overtimeAmount,
                                    BigDecimal deductionsAmount, BigDecimal advanceInstallments, BigDecimal netWage,
                                    String calculationDetailsJson) {
        this(settlementId, workerId, null, null, null, attendanceDays, dailyWage, grossWage, overtimeAmount, deductionsAmount, advanceInstallments, netWage, calculationDetailsJson);
    }

    public ContractorSettlementLine(String settlementId, String workerId, String projectId, String wbsNodeId,
                                    String costCodeId, BigDecimal attendanceDays,
                                    BigDecimal dailyWage, BigDecimal grossWage, BigDecimal overtimeAmount,
                                    BigDecimal deductionsAmount, BigDecimal advanceInstallments, BigDecimal netWage,
                                    String calculationDetailsJson) {
        this.id = UUID.randomUUID().toString();
        this.settlementId = settlementId;
        this.workerId = workerId;
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.attendanceDays = attendanceDays != null ? attendanceDays : BigDecimal.ZERO;
        this.dailyWage = dailyWage != null ? dailyWage : BigDecimal.ZERO;
        this.grossWage = grossWage != null ? grossWage : BigDecimal.ZERO;
        this.overtimeAmount = overtimeAmount != null ? overtimeAmount : BigDecimal.ZERO;
        this.deductionsAmount = deductionsAmount != null ? deductionsAmount : BigDecimal.ZERO;
        this.advanceInstallments = advanceInstallments != null ? advanceInstallments : BigDecimal.ZERO;
        this.netWage = netWage != null ? netWage : BigDecimal.ZERO;
        this.calculationDetailsJson = calculationDetailsJson;
    }

    public void assignProject(String projectId, String wbsNodeId, String costCodeId) {
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
