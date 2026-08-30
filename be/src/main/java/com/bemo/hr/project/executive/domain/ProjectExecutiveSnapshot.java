package com.bemo.hr.project.executive.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_executive_snapshots")
public class ProjectExecutiveSnapshot {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_projects_count", nullable = false)
    private int totalProjectsCount;

    @Column(name = "active_projects_count", nullable = false)
    private int activeProjectsCount;

    @Column(name = "total_contract_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalContractValue;

    @Column(name = "total_budget_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalBudgetValue;

    @Column(name = "total_committed_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalCommittedValue;

    @Column(name = "total_actual_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalActualValue;

    @Column(name = "total_recognized_revenue", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalRecognizedRevenue;

    @Column(name = "total_cash_and_banks", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalCashAndBanks;

    @Column(name = "portfolio_gross_margin_percent", precision = 5, scale = 2)
    private BigDecimal portfolioGrossMarginPercent;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ProjectExecutiveSnapshot() {
    }

    public ProjectExecutiveSnapshot(LocalDate snapshotDate, int totalProjectsCount, int activeProjectsCount,
                                    BigDecimal totalContractValue, BigDecimal totalBudgetValue,
                                    BigDecimal totalCommittedValue, BigDecimal totalActualValue,
                                    BigDecimal totalRecognizedRevenue, BigDecimal totalCashAndBanks,
                                    BigDecimal portfolioGrossMarginPercent) {
        this.id = UUID.randomUUID().toString();
        this.snapshotDate = snapshotDate != null ? snapshotDate : LocalDate.now();
        this.totalProjectsCount = totalProjectsCount;
        this.activeProjectsCount = activeProjectsCount;
        this.totalContractValue = totalContractValue != null ? totalContractValue : BigDecimal.ZERO;
        this.totalBudgetValue = totalBudgetValue != null ? totalBudgetValue : BigDecimal.ZERO;
        this.totalCommittedValue = totalCommittedValue != null ? totalCommittedValue : BigDecimal.ZERO;
        this.totalActualValue = totalActualValue != null ? totalActualValue : BigDecimal.ZERO;
        this.totalRecognizedRevenue = totalRecognizedRevenue != null ? totalRecognizedRevenue : BigDecimal.ZERO;
        this.totalCashAndBanks = totalCashAndBanks != null ? totalCashAndBanks : BigDecimal.ZERO;
        this.portfolioGrossMarginPercent = portfolioGrossMarginPercent != null ? portfolioGrossMarginPercent : BigDecimal.ZERO;
        this.createdAt = System.currentTimeMillis();
    }
}
