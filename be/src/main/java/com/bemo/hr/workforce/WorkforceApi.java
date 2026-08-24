package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class WorkforceApi {
    private WorkforceApi() {
    }

    // Contractor DTOs
    public record ContractorRequest(
            @NotBlank String code,
            @NotBlank String name,
            String tradeName,
            @NotBlank String phone,
            String secondaryPhone,
            String taxId,
            String address,
            @NotBlank String accountingModel,
            String paymentRouting,
            Integer settlementCycleDays,
            BigDecimal defaultDailyRate,
            String feeType,
            BigDecimal feeValue,
            String feeBase,
            BigDecimal fixedPeriodAmount,
            String status,
            String notes
    ) {
    }

    public record ContractorResponse(
            String id,
            String code,
            String name,
            String tradeName,
            String phone,
            String secondaryPhone,
            String taxId,
            String address,
            String accountingModel,
            String paymentRouting,
            int settlementCycleDays,
            BigDecimal defaultDailyRate,
            String feeType,
            BigDecimal feeValue,
            String feeBase,
            BigDecimal fixedPeriodAmount,
            String status,
            String notes,
            long createdAt,
            long updatedAt
    ) {
    }

    // WorkerCategory DTOs
    public record CategoryRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            BigDecimal defaultDailyRate,
            BigDecimal standardDailyHours,
            String defaultSettlementCycle,
            String status,
            String scope
    ) {
    }

    public record CategoryResponse(
            String id,
            String code,
            String name,
            String description,
            BigDecimal defaultDailyRate,
            BigDecimal standardDailyHours,
            String defaultSettlementCycle,
            String status,
            String scope,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
    }

    // Worker DTOs
    public record WorkerRequest(
            @NotBlank String code,
            @NotBlank String fullName,
            @NotBlank String contractorId,
            @NotBlank String categoryId,
            BigDecimal defaultDailyRate,
            BigDecimal standardDailyHours,
            String branchId,
            String attendanceMode,
            String status,
            String phone,
            String nationalId,
            String notes
    ) {
    }

    public record WorkerResponse(
            String id,
            String code,
            String fullName,
            String contractorId,
            String contractorName,
            String categoryId,
            String categoryName,
            BigDecimal defaultDailyRate,
            BigDecimal standardDailyHours,
            String branchId,
            String attendanceMode,
            String status,
            String phone,
            String nationalId,
            String notes,
            long createdAt,
            long updatedAt
    ) {
    }

    // Labor Request DTOs
    public record LaborRequestItemDto(
            String id,
            @NotBlank String categoryId,
            String categoryName,
            int requestedCount,
            int sentCount,
            int acceptedCount,
            int varianceCount
    ) {
    }

    public record LaborRequestCreate(
            @NotBlank String requestNumber,
            String requestDate,
            String branchId,
            String shiftName,
            @NotBlank String contractorId,
            String projectId,
            String wbsNodeId,
            String costCodeId,
            String siteLocation,
            String notes,
            List<LaborRequestItemDto> items
    ) {
    }

    public record LaborRequestResponse(
            String id,
            String requestNumber,
            long requestDate,
            String branchId,
            String shiftName,
            String contractorId,
            String contractorName,
            String projectId,
            String wbsNodeId,
            String costCodeId,
            String siteLocation,
            String status,
            String notes,
            String createdBy,
            String approvedBy,
            List<LaborRequestItemDto> items,
            long createdAt,
            long updatedAt
    ) {
    }

    // Manual Attendance Matrix DTOs
    public record AttendanceCell(
            @NotBlank String workerId,
            @NotBlank String workDate,
            @NotNull BigDecimal attendanceValue,
            String checkIn,
            String checkOut,
            BigDecimal actualHours,
            BigDecimal overtimeHours,
            BigDecimal deductionHours,
            BigDecimal effectiveDailyRate,
            String notes,
            String projectId,
            String wbsNodeId,
            String costCodeId
    ) {
        public AttendanceCell(String workerId, String workDate, BigDecimal attendanceValue,
                              String checkIn, String checkOut, BigDecimal actualHours,
                              BigDecimal overtimeHours, BigDecimal deductionHours,
                              BigDecimal effectiveDailyRate, String notes) {
            this(workerId, workDate, attendanceValue, checkIn, checkOut, actualHours, overtimeHours, deductionHours, effectiveDailyRate, notes, null, null, null);
        }
    }

    public record BatchAttendanceRequest(
            @NotNull List<@Valid AttendanceCell> entries
    ) {
    }

    public record AttendanceCellError(
            String workerId,
            String workDate,
            String field,
            String message
    ) {
    }

    public record BatchAttendanceResponse(
            int createdCount,
            int updatedCount,
            int skippedCount,
            int failedCount,
            List<ManualAttendanceEntry> savedEntries,
            List<AttendanceCellError> errors
    ) {
    }

    // Settlement Period DTOs
    public record SettlementPeriodRequest(
            @NotBlank String periodCode,
            @NotBlank String startDate,
            @NotBlank String endDate,
            String cycleType
    ) {
    }

    public record SettlementPeriodResponse(
            String id,
            String periodCode,
            String startDate,
            String endDate,
            String cycleType,
            String status,
            int calculationVersion,
            Long lastCalculatedAt,
            String lastCalculatedBy,
            Long lastCalculationFailedAt,
            String lastCalculationError,
            boolean needsRecalculation,
            int resultRecordCount,
            BigDecimal resultGrossAmount,
            BigDecimal resultDeductions,
            BigDecimal resultAdvances,
            BigDecimal resultNetAmount,
            int resultWarningCount,
            int resultErrorCount,
            long createdAt,
            long updatedAt
    ) {
    }

    public record SettlementCalculationSummary(
            String periodId,
            String periodCode,
            int totalWorkers,
            int totalContractors,
            BigDecimal totalAttendanceUnits,
            BigDecimal grossWorkersAmount,
            BigDecimal totalDeductions,
            BigDecimal totalAdvanceDeductions,
            BigDecimal netWorkersAmount,
            BigDecimal netContractorsPayable,
            String status,
            int calculationVersion,
            long executedAt,
            String executedBy,
            int warningCount,
            int errorCount,
            List<SettlementIssueResponse> issues
    ) {
    }

    public record SettlementIssueResponse(
            String id, String workerId, String workerName, String severity,
            String code, String message
    ) {
    }

    public record ContractorSettlementLineResponse(
            String id,
            String settlementId,
            String workerId,
            String workerName,
            String projectId,
            String wbsNodeId,
            String costCodeId,
            BigDecimal attendanceDays,
            BigDecimal dailyWage,
            BigDecimal grossWage,
            BigDecimal overtimeAmount,
            BigDecimal deductionsAmount,
            BigDecimal advanceInstallments,
            BigDecimal netWage
    ) {
    }

    public record ContractorSettlementAdjustmentResponse(
            String id,
            String settlementId,
            String adjustmentType,
            String description,
            BigDecimal amount,
            String reason,
            String createdBy,
            long createdAt
    ) {
    }

    public record ContractorSettlementDetailResponse(
            String id,
            String periodId,
            String contractorId,
            String contractorName,
            String accountingModel,
            BigDecimal workersNetTotal,
            BigDecimal contractorRatesTotal,
            BigDecimal commissionAmount,
            BigDecimal fixedAmount,
            BigDecimal additionsAmount,
            BigDecimal deductionsAmount,
            BigDecimal grossAmount,
            BigDecimal netPayable,
            BigDecimal paidAmount,
            String invoiceNumber,
            Long invoiceDate,
            String postedJournalEntryId,
            String status,
            Long version,
            List<ContractorSettlementLineResponse> lines,
            List<ContractorSettlementAdjustmentResponse> adjustments,
            long createdAt,
            long updatedAt
    ) {
    }

    public record LinkInvoiceRequest(
            @NotBlank String invoiceNumber,
            @NotNull Long invoiceDate,
            BigDecimal invoiceAmount,
            String notes
    ) {
    }

    public record SettlementPostingRequest(
            @NotBlank String operationId,
            @NotNull Long expectedVersion,
            String reason
    ) {
    }

    public record RecordSettlementPaymentRequest(
            @NotBlank String operationId,
            @NotNull BigDecimal amount,
            Long paymentDate,
            String paymentReference,
            String notes
    ) {
    }

    // --- Bulk Attendance Update DTOs ---
    public record BulkUpdateAttendanceRequest(
            @NotBlank String workDate,
            @NotNull List<@NotBlank String> workerIds,
            @NotNull BigDecimal attendanceValue,
            boolean overrideExisting
    ) {
    }

    public record BulkUpdateAttendanceResponse(
            int updatedCount
    ) {
    }

    public record CalculationRulesResponse(
            BigDecimal overtimeMultiplier,
            BigDecimal standardHours,
            BigDecimal minimumHoursForOvertime,
            BigDecimal defaultOvertimeRate,
            String standardWorkHours,
            String notes
    ) {
    }

    // --- Advances DTOs ---
    public record AdvanceCreateRequest(
            String recipientType,
            String workerId,
            String contractorId,
            String employeeId,
            BigDecimal amount,
            String termType,
            Integer totalInstallments,
            BigDecimal installmentAmount,
            String deductionFrequency,
            BigDecimal maxDeductionPercent,
            String reason,
            String firstInstallmentDate,
            String deductionMode,
            Integer deferralPeriods
    ) {
    }

    public record AdvanceResponse(
            String id,
            String recipientType,
            String workerId,
            String workerName,
            String contractorId,
            String contractorName,
            String employeeId,
            String employeeName,
            BigDecimal amount,
            String termType,
            int totalInstallments,
            BigDecimal installmentAmount,
            BigDecimal remainingBalance,
            String deductionFrequency,
            BigDecimal maxDeductionPercent,
            String status,
            String reason,
            String firstInstallmentDate,
            String deductionMode,
            int deferralPeriods,
            String appliedPolicyId,
            Integer appliedPolicyVersion,
            String appliedPolicySnapshot,
            long createdAt
    ) {
    }

    public record AdvanceRepayRequest(
            BigDecimal amount,
            String repaymentType,
            String repaymentDate,
            String paymentMethod,
            String receiptRef,
            String notes
    ) {
    }

    public record AdvancePolicyRequest(
            String id,
            String scopeType,
            String scopeId,
            String deductionMode,
            String deductionFrequency,
            BigDecimal maxDeductionPercent,
            Integer defaultInstallments,
            Integer deferralPeriods,
            String effectiveFrom,
            String effectiveTo,
            Boolean active
    ) {
    }

    public record AdvancePolicyResponse(
            String id,
            String scopeType,
            String scopeId,
            String scopeName,
            String deductionMode,
            String deductionFrequency,
            BigDecimal maxDeductionPercent,
            int defaultInstallments,
            int deferralPeriods,
            long version,
            String effectiveFrom,
            String effectiveTo,
            boolean active,
            long updatedAt
    ) {
    }

    // --- Advance Deduction Policy DTOs (WP-07) ---
    public record ResolvedDeductionPolicyResponse(
            String mode,
            String cadence,
            String source,
            String policyId,
            Long policyVersion,
            boolean manual
    ) {
    }

    public record ManualDeductionRequest(
            @jakarta.validation.constraints.NotBlank String employeeId,
            @jakarta.validation.constraints.NotBlank String periodId
    ) {
    }

    public record ManualDeductionResult(
            String employeeId,
            String periodId,
            BigDecimal appliedAmount,
            boolean duplicate,
            List<ManualDeductionLine> lines
    ) {
    }

    public record ManualDeductionLine(
            String advanceId,
            BigDecimal appliedAmount
    ) {
    }

    // --- Project Labor Cost Report DTOs ---
    public record ProjectLaborCostItem(
            String workerId,
            String workerCode,
            String workerName,
            String contractorId,
            String contractorName,
            String wbsNodeId,
            String costCodeId,
            BigDecimal attendanceDays,
            BigDecimal dailyWage,
            BigDecimal grossCost,
            BigDecimal overtimeAmount,
            BigDecimal netCost
    ) {
    }

    public record ProjectLaborCostReportResponse(
            String projectId,
            String projectName,
            String periodId,
            int totalWorkersCount,
            BigDecimal totalAttendanceDays,
            BigDecimal totalGrossLaborCost,
            BigDecimal totalOvertimeAmount,
            BigDecimal totalNetLaborCost,
            List<ProjectLaborCostItem> items
    ) {
    }
}
