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
}
