package com.bemo.hr.organization.api;

import com.bemo.hr.organization.domain.IntercompanyStatus;
import com.bemo.hr.organization.domain.IntercompanyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class OrganizationApi {

    public record CompanyResponse(
            String id,
            String code,
            String name,
            String taxNumber,
            String commercialRegistry,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
    }

    public record CompanyPayload(
            @NotBlank String code,
            @NotBlank String name,
            String taxNumber,
            String commercialRegistry,
            boolean active
    ) {
    }

    public record BranchResponse(
            String id,
            String companyId,
            String code,
            String name,
            String location,
            boolean active,
            boolean isMainBranch,
            String phone,
            String email,
            String taxNumber,
            String commercialRegistry,
            String defaultWarehouseId,
            String defaultCashboxId,
            String defaultBankAccountId,
            String defaultPosTerminalId,
            String documentCodePrefix,
            long createdAt,
            long updatedAt
    ) {
    }

    public record BranchPayload(
            @NotBlank String companyId,
            @NotBlank String code,
            @NotBlank String name,
            String location,
            boolean active,
            boolean isMainBranch,
            String phone,
            String email,
            String taxNumber,
            String commercialRegistry,
            String defaultWarehouseId,
            String defaultCashboxId,
            String defaultBankAccountId,
            String defaultPosTerminalId,
            String documentCodePrefix
    ) {
    }

    public record WarehouseResponse(
            String id,
            String branchId,
            String code,
            String name,
            String location,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
    }

    public record WarehousePayload(
            @NotBlank String branchId,
            @NotBlank String code,
            @NotBlank String name,
            String location,
            boolean active
    ) {
    }

    public record DepartmentResponse(
            String id,
            String companyId,
            String code,
            String name,
            String managerId,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
    }

    public record DepartmentPayload(
            @NotBlank String companyId,
            @NotBlank String code,
            @NotBlank String name,
            String managerId,
            boolean active
    ) {
    }

    public record OrganizationHierarchyResponse(
            List<CompanyResponse> companies,
            List<BranchResponse> branches,
            List<WarehouseResponse> warehouses,
            List<DepartmentResponse> departments
    ) {
    }

    // --- Intercompany & Consolidation DTOs ---

    public record IntercompanyTransactionResponse(
            String id,
            String transactionNumber,
            String fromCompanyId,
            String fromCompanyName,
            String fromBranchId,
            String fromBranchName,
            String toCompanyId,
            String toCompanyName,
            String toBranchId,
            String toBranchName,
            IntercompanyType transactionType,
            BigDecimal amount,
            String currency,
            String description,
            String dueToAccountId,
            String dueFromAccountId,
            IntercompanyStatus status,
            String eliminatedInPeriod,
            String journalEntryId,
            long createdAt,
            long updatedAt
    ) {
    }

    public record CreateIntercompanyPayload(
            @NotBlank String fromCompanyId,
            String fromBranchId,
            @NotBlank String toCompanyId,
            String toBranchId,
            @NotNull IntercompanyType transactionType,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String currency,
            String description,
            String dueToAccountId,
            String dueFromAccountId
    ) {
    }

    public record RunEliminationPayload(
            @NotBlank String period
    ) {
    }

    public record EliminationResultResponse(
            String period,
            int eliminatedCount,
            BigDecimal eliminatedTotalAmount
    ) {
    }

    public record BranchPerformanceMetric(
            String branchId,
            String branchCode,
            String branchName,
            String companyId,
            String companyName,
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal netProfit,
            BigDecimal marginPercent,
            BigDecimal inventoryValue,
            int headcount,
            int activeProjects
    ) {
    }

    public record ConsolidatedOrganizationSummary(
            BigDecimal totalRevenue,
            BigDecimal totalExpenses,
            BigDecimal eliminatedTransfers,
            BigDecimal consolidatedNetMargin,
            int activeBranches,
            int totalHeadcount,
            List<BranchPerformanceMetric> branchMetrics
    ) {
    }

    public record BranchControlSummary(
            String branchId,
            String branchCode,
            String branchName,
            String companyId,
            String companyName,
            boolean isMainBranch,
            int warehouseCount,
            int cashboxCount,
            int bankAccountCount,
            int posTerminalCount,
            int employeeCount,
            BigDecimal inventoryValuation,
            int activeTransfersCount
    ) {
    }

    public record BranchComparisonItem(
            String branchId,
            String branchCode,
            String branchName,
            String companyName,
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal netProfit,
            BigDecimal marginPct,
            BigDecimal inventoryValuation,
            BigDecimal cashBalance,
            int headcount
    ) {
    }

    public record GroupPlLine(
            String category,
            String lineName,
            BigDecimal amount,
            java.util.Map<String, BigDecimal> branchBreakdown,
            BigDecimal eliminations,
            BigDecimal consolidatedAmount
    ) {
    }

    public record GroupBalanceSheetLine(
            String classification,
            String lineName,
            BigDecimal amount,
            java.util.Map<String, BigDecimal> branchBreakdown,
            BigDecimal eliminations,
            BigDecimal consolidatedAmount
    ) {
    }

    public record ConsolidatedGroupReport(
            String companyId,
            String companyName,
            String branchId,
            String branchName,
            String period,
            BigDecimal totalRevenue,
            BigDecimal totalCogs,
            BigDecimal grossProfit,
            BigDecimal grossMarginPct,
            BigDecimal totalOperatingExpenses,
            BigDecimal netOperatingProfit,
            BigDecimal totalInventoryValuation,
            BigDecimal totalCashBankBalance,
            int totalHeadcount,
            List<BranchComparisonItem> branchComparison,
            List<GroupPlLine> plLines,
            List<GroupBalanceSheetLine> balanceSheetLines
    ) {
    }
}
