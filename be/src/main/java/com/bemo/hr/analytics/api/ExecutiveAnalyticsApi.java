package com.bemo.hr.analytics.api;

import com.bemo.hr.analytics.domain.KpiCategory;
import com.bemo.hr.analytics.domain.KpiGrain;
import com.bemo.hr.analytics.domain.KpiUnit;
import com.bemo.hr.analytics.domain.ReconciliationStatus;
import com.bemo.hr.analytics.domain.TrendDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ExecutiveAnalyticsApi {
    private ExecutiveAnalyticsApi() {}

    public record KpiDefinitionResponse(
            String key,
            String nameEn,
            String nameAr,
            KpiCategory category,
            KpiGrain grain,
            KpiUnit unit,
            String formulaEn,
            String formulaAr,
            String sourceModule,
            String requiredPermission
    ) {}

    public record ExecutiveKpiCard(
            String key,
            String nameEn,
            String nameAr,
            KpiCategory category,
            BigDecimal actualValue,
            BigDecimal targetValue,
            BigDecimal variancePercent,
            TrendDirection trendDirection,
            KpiUnit unit,
            ReconciliationStatus reconciliationStatus,
            String drilldownUrl
    ) {}

    public record ModuleSummary(
            KpiCategory category,
            String moduleName,
            List<ExecutiveKpiCard> kpis
    ) {}

    public record ExecutiveOverviewResponse(
            String period,
            long timestamp,
            BigDecimal totalRevenue,
            BigDecimal totalOpex,
            BigDecimal grossProfit,
            BigDecimal netMarginPercent,
            BigDecimal operatingCashFlow,
            BigDecimal salesBookings,
            BigDecimal posGross,
            BigDecimal openReceivables,
            BigDecimal inventoryValuation,
            BigDecimal projectPortfolioValue,
            BigDecimal projectCostVariance,
            int activeHeadcount,
            BigDecimal payrollDisbursed,
            BigDecimal attendanceRatePercent,
            BigDecimal etaTaxCompliancePercent,
            List<ModuleSummary> moduleSummaries
    ) {}

    public record TrendPeriodPoint(
            String period,
            BigDecimal revenue,
            BigDecimal opex,
            BigDecimal netProfit,
            BigDecimal marginPercent,
            BigDecimal salesBookings,
            BigDecimal inventoryValue,
            BigDecimal payrollDisbursed,
            BigDecimal projectEarnedValue
    ) {}

    public record ComparativeTrendsResponse(
            int months,
            List<TrendPeriodPoint> trendPoints
    ) {}

    public record CreateSnapshotPayload(
            @NotBlank String periodKey,
            @NotNull KpiCategory category,
            @NotBlank String kpiKey,
            BigDecimal targetValue,
            @NotNull BigDecimal actualValue,
            BigDecimal varianceValue,
            BigDecimal variancePercent,
            TrendDirection trendDirection,
            ReconciliationStatus reconciliationStatus,
            String drilldownUrl,
            String metadataJson
    ) {}

    public record ExecutiveKpiSnapshotResponse(
            String id,
            long snapshotDate,
            String periodKey,
            KpiCategory category,
            String kpiKey,
            BigDecimal targetValue,
            BigDecimal actualValue,
            BigDecimal varianceValue,
            BigDecimal variancePercent,
            TrendDirection trendDirection,
            ReconciliationStatus reconciliationStatus,
            String drilldownUrl,
            String metadataJson,
            long createdAt
    ) {}

    public record AgingBucket(
            String labelKey,
            BigDecimal amount,
            int invoiceCount,
            BigDecimal percentOfTotal
    ) {}

    public record ArApAgingSummary(
            AgingBucket current,
            AgingBucket days30To60,
            AgingBucket days60To90,
            AgingBucket daysOver90,
            BigDecimal total,
            BigDecimal totalOverdue
    ) {}

    public record BranchPerformanceItem(
            String branchId,
            String branchCode,
            String branchName,
            boolean isMainBranch,
            BigDecimal revenue,
            BigDecimal cogs,
            BigDecimal grossProfit,
            BigDecimal grossMarginPercent,
            BigDecimal opex,
            BigDecimal netProfit,
            int headcount,
            BigDecimal cashAndBank
    ) {}

    public record TopCustomerItem(
            String customerId,
            String customerName,
            BigDecimal totalInvoiced,
            BigDecimal totalCollected,
            BigDecimal outstandingBalance,
            int invoiceCount
    ) {}

    public record TopProductItem(
            String itemId,
            String itemCode,
            String itemName,
            BigDecimal quantitySold,
            BigDecimal revenue,
            BigDecimal cogs,
            BigDecimal marginPercent
    ) {}

    public record ExpenseCategoryItem(
            String category,
            String nameKey,
            BigDecimal amount,
            BigDecimal percentOfTotal
    ) {}

    public record StockAlertItem(
            String itemId,
            String itemCode,
            String itemName,
            BigDecimal currentStock,
            BigDecimal reorderPoint,
            BigDecimal reorderQuantity,
            boolean isDeadStock,
            BigDecimal estimatedValue
    ) {}

    public record ManufacturingWipItem(
            String orderId,
            String orderNumber,
            String itemName,
            BigDecimal targetQuantity,
            BigDecimal actualOutputQuantity,
            BigDecimal materialCost,
            String startDate,
            String status
    ) {}

    public record ProjectBudgetVarianceItem(
            String projectId,
            String code,
            String name,
            BigDecimal contractValue,
            BigDecimal budgetAmount,
            BigDecimal actualCost,
            BigDecimal costVariance,
            String status
    ) {}

    public record CockpitTargetResponse(
            String id,
            String periodKey,
            BigDecimal targetRevenue,
            BigDecimal targetGrossMarginPercent,
            BigDecimal targetMaxOpex,
            BigDecimal targetMinLiquidity,
            BigDecimal targetMaxOverdueAr,
            String notes,
            long updatedAt
    ) {}

    public record SaveCockpitTargetRequest(
            @NotBlank String periodKey,
            BigDecimal targetRevenue,
            BigDecimal targetGrossMarginPercent,
            BigDecimal targetMaxOpex,
            BigDecimal targetMinLiquidity,
            BigDecimal targetMaxOverdueAr,
            String notes
    ) {}

    public record OwnerCockpitKpiSummary(
            BigDecimal todaySales,
            BigDecimal todayCollections,
            BigDecimal netLiquidity,
            BigDecimal cashInHand,
            BigDecimal bankBalances,
            BigDecimal totalRevenue,
            BigDecimal totalCogs,
            BigDecimal grossMarginAmount,
            BigDecimal grossMarginPercent,
            BigDecimal totalOpex,
            BigDecimal operatingProfit,
            BigDecimal netProfit,
            BigDecimal netMarginPercent,
            BigDecimal payrollDisbursed,
            BigDecimal payrollPending,
            int activeHeadcount,
            int manufacturingWipCount,
            BigDecimal manufacturingWipValuation,
            BigDecimal projectBudgetTotal,
            BigDecimal projectActualCost,
            BigDecimal projectCostVariance,
            int lowStockCount,
            int deadStockCount,
            BigDecimal totalReceivables,
            BigDecimal overdueReceivables,
            BigDecimal totalPayables,
            BigDecimal overduePayables
    ) {}

    public record OwnerCockpitResponse(
            String period,
            String companyId,
            String branchId,
            long timestamp,
            OwnerCockpitKpiSummary kpiSummary,
            ArApAgingSummary arAging,
            ArApAgingSummary apAging,
            List<BranchPerformanceItem> branchLeaderboard,
            List<TopCustomerItem> topCustomers,
            List<TopProductItem> topProducts,
            List<ExpenseCategoryItem> expenseBreakdown,
            List<StockAlertItem> lowStockAlerts,
            List<StockAlertItem> deadStockAlerts,
            List<ManufacturingWipItem> manufacturingWip,
            List<ProjectBudgetVarianceItem> projectBudgetControl,
            CockpitTargetResponse targets
    ) {}
}

