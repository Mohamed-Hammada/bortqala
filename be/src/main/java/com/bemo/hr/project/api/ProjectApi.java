package com.bemo.hr.project.api;

import com.bemo.hr.project.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ProjectApi {

    private ProjectApi() {
    }

    public record ProjectResponse(
            String id,
            String code,
            String name,
            String nameEn,
            String description,
            String companyId,
            String branchId,
            String ownerPartyId,
            String projectManagerId,
            String siteAddress,
            String contractNumber,
            BigDecimal contractValue,
            String currencyCode,
            Long startDate,
            Long endDate,
            ProjectStatus status,
            boolean budgetBlocking,
            boolean active,
            long createdAt,
            long updatedAt,
            long version,
            BigDecimal totalPlannedAmount,
            int wbsCount
    ) {}

    public record CreateProjectRequest(
            @NotBlank(message = "Project code is required.")
            @Size(max = 50, message = "Project code cannot exceed 50 characters.")
            String code,

            @NotBlank(message = "Project name is required.")
            @Size(max = 255, message = "Project name cannot exceed 255 characters.")
            String name,

            @Size(max = 255)
            String nameEn,

            String description,
            String companyId,
            String branchId,
            String ownerPartyId,
            String projectManagerId,
            String siteAddress,
            String contractNumber,

            @PositiveOrZero(message = "Contract value must be non-negative.")
            BigDecimal contractValue,

            String currencyCode,
            Long startDate,
            Long endDate,
            Boolean budgetBlocking
    ) {}

    public record UpdateProjectRequest(
            @NotBlank(message = "Project name is required.")
            @Size(max = 255, message = "Project name cannot exceed 255 characters.")
            String name,

            @Size(max = 255)
            String nameEn,

            String description,
            String companyId,
            String branchId,
            String ownerPartyId,
            String projectManagerId,
            String siteAddress,
            String contractNumber,

            @PositiveOrZero(message = "Contract value must be non-negative.")
            BigDecimal contractValue,

            String currencyCode,
            Long startDate,
            Long endDate,
            Boolean budgetBlocking
    ) {}

    public record ProjectSummaryResponse(
            long totalProjects,
            long activeProjects,
            long onHoldProjects,
            long completedProjects,
            long closedProjects,
            BigDecimal totalContractValue,
            BigDecimal totalPlannedAmount
    ) {}

    public record WbsNodeResponse(
            String id,
            String projectId,
            String parentId,
            String wbsCode,
            String wbsPath,
            String name,
            String nameEn,
            String description,
            WbsNodeType nodeType,
            int level,
            int sortOrder,
            String unitOfMeasure,
            BigDecimal plannedQuantity,
            BigDecimal unitRate,
            BigDecimal plannedAmount,
            String costCodeId,
            Long startDate,
            Long endDate,
            WbsNodeStatus status,
            long createdAt,
            long updatedAt,
            long version,
            List<WbsNodeResponse> children
    ) {}

    public record CreateWbsNodeRequest(
            String parentId,

            @NotBlank(message = "WBS code is required.")
            @Size(max = 50)
            String wbsCode,

            @NotBlank(message = "WBS node name is required.")
            @Size(max = 255)
            String name,

            @Size(max = 255)
            String nameEn,

            String description,
            WbsNodeType nodeType,
            Integer sortOrder,
            String unitOfMeasure,

            @PositiveOrZero
            BigDecimal plannedQuantity,

            @PositiveOrZero
            BigDecimal unitRate,

            String costCodeId,
            Long startDate,
            Long endDate,
            WbsNodeStatus status
    ) {}

    public record UpdateWbsNodeRequest(
            @NotBlank(message = "WBS node name is required.")
            @Size(max = 255)
            String name,

            @Size(max = 255)
            String nameEn,

            String description,
            WbsNodeType nodeType,
            Integer sortOrder,
            String unitOfMeasure,

            @PositiveOrZero
            BigDecimal plannedQuantity,

            @PositiveOrZero
            BigDecimal unitRate,

            String costCodeId,
            Long startDate,
            Long endDate,
            WbsNodeStatus status
    ) {}

    public record RepositionWbsNodeRequest(
            String parentId,
            int sortOrder
    ) {}

    public record ProjectCostCodeResponse(
            String id,
            String code,
            String name,
            String nameEn,
            CostCodeCategory category,
            String description,
            boolean active,
            long createdAt,
            long updatedAt,
            long version
    ) {}

    public record CreateCostCodeRequest(
            @NotBlank(message = "Cost code is required.")
            @Size(max = 50)
            String code,

            @NotBlank(message = "Cost code name is required.")
            @Size(max = 255)
            String name,

            @Size(max = 255)
            String nameEn,

            @NotNull(message = "Category is required.")
            CostCodeCategory category,

            String description
    ) {}

    public record UpdateCostCodeRequest(
            @NotBlank(message = "Cost code name is required.")
            @Size(max = 255)
            String name,

            @Size(max = 255)
            String nameEn,

            @NotNull(message = "Category is required.")
            CostCodeCategory category,

            String description,
            Boolean active
    ) {}

    public record ProjectPartyRoleResponse(
            String id,
            String projectId,
            String partyId,
            ProjectPartyRoleType roleType,
            String notes,
            long createdAt
    ) {}

    public record AssignPartyRoleRequest(
            @NotBlank(message = "Party ID is required.")
            String partyId,

            @NotNull(message = "Role type is required.")
            ProjectPartyRoleType roleType,

            String notes
    ) {}

    // Site Custody Register DTOs
    public record SiteCustodyResponse(
            String id,
            String projectId,
            String custodyCode,
            String custodianEmployeeId,
            String custodianName,
            String custodyType,
            BigDecimal initialAmount,
            BigDecimal remainingBalance,
            String status,
            long issuedAt,
            Long settledAt,
            String notes,
            long version,
            long createdAt,
            long updatedAt,
            List<SiteCustodyExpenseResponse> expenses,
            List<SiteCustodyReturnResponse> returns
    ) {}

    public record IssueCustodyRequest(
            @NotBlank(message = "Custody code is required.")
            String custodyCode,

            String custodianEmployeeId,

            @NotBlank(message = "Custodian name is required.")
            String custodianName,

            @NotBlank(message = "Custody type is required (CASH, MATERIAL, EQUIPMENT).")
            String custodyType,

            @NotNull(message = "Initial amount is required.")
            @PositiveOrZero(message = "Initial amount must be non-negative.")
            BigDecimal initialAmount,

            String notes
    ) {}

    public record SiteCustodyExpenseResponse(
            String id,
            String custodyId,
            long expenseDate,
            BigDecimal amount,
            String category,
            String description,
            String receiptNumber,
            String recordedBy,
            String status,
            long createdAt,
            long updatedAt
    ) {}

    public record RecordCustodyExpenseRequest(
            long expenseDate,

            @NotNull(message = "Amount is required.")
            @PositiveOrZero(message = "Amount must be positive.")
            BigDecimal amount,

            @NotBlank(message = "Category is required.")
            String category,

            @NotBlank(message = "Description is required.")
            String description,

            String receiptNumber,
            String recordedBy
    ) {}

    public record SiteCustodyReturnResponse(
            String id,
            String custodyId,
            long returnDate,
            BigDecimal amountReturned,
            String receivedBy,
            String notes,
            long createdAt
    ) {}

    public record SettleCustodyRequest(
            @NotNull(message = "Amount returned is required.")
            @PositiveOrZero
            BigDecimal amountReturned,

            String receivedBy,
            String notes
    ) {}
}
