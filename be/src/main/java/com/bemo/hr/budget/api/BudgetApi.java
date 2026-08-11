package com.bemo.hr.budget.api;

import com.bemo.hr.budget.domain.BudgetPeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BudgetApi {

    public record BudgetPayload(
            @NotNull @Min(2000) Integer fiscalYear,
            BudgetPeriodType periodType,
            Integer periodMonth,
            @NotBlank String departmentId,
            @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal plannedAmount,
            String currencyCode,
            Boolean blocking,
            Boolean active
    ) {}

    public record BudgetResponse(
            String id,
            int fiscalYear,
            BudgetPeriodType periodType,
            Integer periodMonth,
            String departmentId,
            String departmentName,
            BigDecimal plannedAmount,
            String currencyCode,
            boolean blocking,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record BudgetStatusResponse(
            String budgetId,
            int fiscalYear,
            BudgetPeriodType periodType,
            Integer periodMonth,
            String departmentId,
            String departmentName,
            BigDecimal plannedAmount,
            BigDecimal committedAmount,
            BigDecimal actualAmount,
            BigDecimal availableAmount,
            BigDecimal utilizationPercent,
            boolean blocking,
            String currencyCode
    ) {}

    public record EncumbranceResponse(
            String id,
            String budgetId,
            String purchaseOrderId,
            String purchaseOrderNumber,
            String documentType,
            String status,
            BigDecimal committedAmount,
            BigDecimal liquidatedAmount,
            BigDecimal releasedAmount,
            String currencyCode,
            long committedAt,
            Long releasedAt
    ) {}
}
