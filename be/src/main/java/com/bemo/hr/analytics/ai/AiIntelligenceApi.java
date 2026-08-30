package com.bemo.hr.analytics.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AiIntelligenceApi {

    private AiIntelligenceApi() {
    }

    public record CashFlowPoint(
            int year,
            int month,
            String periodLabel,
            BigDecimal projectedInflow,
            BigDecimal projectedOutflow,
            BigDecimal projectedNet,
            BigDecimal lowerBand,
            BigDecimal upperBand,
            boolean historical
    ) {
    }

    public record CashFlowForecastResponse(
            int forecastMonths,
            List<CashFlowPoint> points,
            BigDecimal totalProjectedNet,
            String confidenceNote
    ) {
    }

    public record ExpenseAnomalyDto(
            String vendorId,
            String vendorName,
            String expenseCategory,
            BigDecimal currentAmount,
            BigDecimal sixMonthMean,
            BigDecimal standardDeviation,
            BigDecimal zScore,
            String flaggedReason,
            long transactionTimestamp
    ) {
    }

    public record DemandForecastDto(
            String itemId,
            String itemCode,
            String itemName,
            BigDecimal currentStock,
            BigDecimal monthlyAvgConsumption,
            int leadTimeDays,
            BigDecimal safetyStock,
            BigDecimal suggestedReorderQty,
            String urgencyLevel
    ) {
    }

    public record CollectionsRiskDto(
            String customerId,
            String customerName,
            BigDecimal outstandingBalance,
            int totalInvoices,
            int overdueInvoices,
            BigDecimal avgDaysOverdue,
            String riskBand, // "A" (Low Risk), "B" (Medium Risk), "C" (High Risk)
            List<String> scoringFactors
    ) {
    }

    public record NlQueryRequest(
            @NotBlank String question,
            String datasetKey
    ) {
    }

    public record NlQueryResponse(
            String question,
            String targetDataset,
            String interpretedIntent,
            List<String> appliedFilters,
            List<Map<String, Object>> records,
            int totalMatchingRows,
            String summaryAnswer,
            boolean success
    ) {
    }
}
