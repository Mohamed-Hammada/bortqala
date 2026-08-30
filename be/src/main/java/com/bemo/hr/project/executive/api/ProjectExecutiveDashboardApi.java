package com.bemo.hr.project.executive.api;

import com.bemo.hr.project.domain.ProjectStatus;

import java.math.BigDecimal;
import java.util.List;

public final class ProjectExecutiveDashboardApi {

    private ProjectExecutiveDashboardApi() {
    }

    public record ProjectExecutiveDashboardResponse(
            int totalProjects,
            int activeProjects,
            BigDecimal totalContractValue,
            BigDecimal totalBudget,
            BigDecimal totalCommitted,
            BigDecimal totalActualCost,
            BigDecimal totalRevenue,
            BigDecimal portfolioGrossProfit,
            BigDecimal portfolioGrossMarginPercent,
            BigDecimal totalReceivables,
            BigDecimal totalRetentionHeld,
            TreasurySummaryResponse treasury,
            ExecutionHealthResponse executionHealth,
            List<ProjectMatrixRowResponse> projects,
            String currencyCode,
            long dataAsOf
    ) {}

    public record TreasurySummaryResponse(
            BigDecimal totalBankBalance,
            BigDecimal totalCashOnHand,
            BigDecimal totalUnclearedCheques,
            BigDecimal netLiquidCapital
    ) {}

    public record ExecutionHealthResponse(
            BigDecimal averageProgressPercent,
            int delayedProjectsCount,
            int activeWorkforceHeadcount,
            int criticalTasksCount
    ) {}

    public record ProjectMatrixRowResponse(
            String projectId,
            String projectName,
            ProjectStatus status,
            BigDecimal contractValue,
            BigDecimal budgetAmount,
            BigDecimal committedAmount,
            BigDecimal actualCost,
            BigDecimal recognizedRevenue,
            BigDecimal grossProfit,
            BigDecimal grossMarginPercent,
            BigDecimal progressPercent,
            boolean delayed
    ) {}
}
