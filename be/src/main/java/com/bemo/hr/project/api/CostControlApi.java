package com.bemo.hr.project.api;

import com.bemo.hr.project.domain.BudgetVersionStatus;
import com.bemo.hr.project.domain.CostCategory;
import com.bemo.hr.project.domain.CostLedgerEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CostControlApi {

    private CostControlApi() {
    }

    public record CostControlSummaryResponse(
            String projectId,
            String projectName,
            BigDecimal contractValue,
            String currencyCode,
            BigDecimal totalBudget,
            BigDecimal totalCommitted,
            BigDecimal totalActualCost,
            BigDecimal totalRecognizedRevenue,
            BigDecimal currentGrossProfit,
            BigDecimal currentGrossMarginPercent,
            BigDecimal forecastEac,
            BigDecimal forecastVac,
            BigDecimal forecastProfit,
            BigDecimal forecastMarginPercent,
            List<CostCategoryBreakdownResponse> categoryBreakdowns
    ) {}

    public record CostCategoryBreakdownResponse(
            CostCategory category,
            BigDecimal budgetAmount,
            BigDecimal committedAmount,
            BigDecimal actualAmount,
            BigDecimal varianceAmount
    ) {}

    public record ProjectBudgetVersionResponse(
            String id,
            String projectId,
            int versionNumber,
            String versionName,
            BudgetVersionStatus status,
            String approvedByUserId,
            Long approvedAt,
            BigDecimal totalBudgetAmount,
            String notes,
            int linesCount,
            List<ProjectBudgetLineResponse> lines
    ) {}

    public record ProjectBudgetLineResponse(
            String id,
            String budgetVersionId,
            String projectId,
            String wbsNodeId,
            String costCodeId,
            CostCategory costCategory,
            String description,
            BigDecimal budgetQuantity,
            String unitOfMeasure,
            BigDecimal budgetUnitRate,
            BigDecimal budgetAmount,
            int sortOrder
    ) {}

    public record ProjectCostLedgerEntryResponse(
            String id,
            String projectId,
            String wbsNodeId,
            String wbsCode,
            String costCodeId,
            CostCategory costCategory,
            CostLedgerEntryType entryType,
            String sourceModule,
            String sourceDocumentId,
            String sourceDocumentNumber,
            LocalDate entryDate,
            String description,
            BigDecimal quantity,
            BigDecimal unitRate,
            BigDecimal amount,
            String currencyCode,
            Long postedAt
    ) {}

    public record ProjectForecastEacResponse(
            String id,
            String projectId,
            String wbsNodeId,
            String wbsCode,
            String wbsName,
            String costCodeId,
            CostCategory costCategory,
            BigDecimal budgetAmount,
            BigDecimal actualCostToDate,
            BigDecimal committedCost,
            BigDecimal estimateToComplete,
            BigDecimal estimateAtCompletion,
            BigDecimal varianceAtCompletion,
            BigDecimal forecastProfitMarginPercent,
            String notes
    ) {}

    public record CreateBudgetVersionRequest(
            @NotBlank String versionName,
            String notes,
            boolean initFromWbs,
            List<SaveBudgetLineRequest> lines
    ) {}

    public record SaveBudgetLineRequest(
            String id,
            String wbsNodeId,
            String costCodeId,
            @NotNull CostCategory costCategory,
            @NotBlank String description,
            BigDecimal budgetQuantity,
            String unitOfMeasure,
            @NotNull BigDecimal budgetUnitRate,
            int sortOrder
    ) {}

    public record UpdateForecastEacRequest(
            @NotBlank String wbsNodeId,
            @NotNull BigDecimal estimateToComplete,
            String notes
    ) {}

    public record RecordCostLedgerEntryRequest(
            String wbsNodeId,
            String costCodeId,
            @NotNull CostCategory costCategory,
            @NotNull CostLedgerEntryType entryType,
            @NotBlank String sourceModule,
            String sourceDocumentId,
            String sourceDocumentNumber,
            @NotNull LocalDate entryDate,
            @NotBlank String description,
            BigDecimal quantity,
            BigDecimal unitRate,
            @NotNull BigDecimal amount,
            String currencyCode
    ) {}
}
