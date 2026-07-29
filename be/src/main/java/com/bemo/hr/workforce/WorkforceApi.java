package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class WorkforceApi {
    private WorkforceApi() { }

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
    ) { }

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
    ) { }

    // WorkerCategory DTOs
    public record CategoryRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        BigDecimal defaultDailyRate,
        BigDecimal standardDailyHours,
        String defaultSettlementCycle,
        String status
    ) { }

    public record CategoryResponse(
        String id,
        String code,
        String name,
        String description,
        BigDecimal defaultDailyRate,
        BigDecimal standardDailyHours,
        String defaultSettlementCycle,
        String status,
        long createdAt,
        long updatedAt
    ) { }

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
    ) { }

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
    ) { }

    // Labor Request DTOs
    public record LaborRequestItemDto(
        String id,
        @NotBlank String categoryId,
        String categoryName,
        int requestedCount,
        int sentCount,
        int acceptedCount,
        int varianceCount
    ) { }

    public record LaborRequestCreate(
        @NotBlank String requestNumber,
        String requestDate,
        String branchId,
        String shiftName,
        @NotBlank String contractorId,
        String notes,
        List<LaborRequestItemDto> items
    ) { }

    public record LaborRequestResponse(
        String id,
        String requestNumber,
        long requestDate,
        String branchId,
        String shiftName,
        String contractorId,
        String contractorName,
        String status,
        String notes,
        String createdBy,
        String approvedBy,
        List<LaborRequestItemDto> items,
        long createdAt,
        long updatedAt
    ) { }

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
        String notes
    ) { }

    public record BatchAttendanceRequest(
        @NotNull List<@Valid AttendanceCell> entries
    ) { }

    public record AttendanceCellError(
        String workerId,
        String workDate,
        String field,
        String message
    ) { }

    public record BatchAttendanceResponse(
        int createdCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        List<ManualAttendanceEntry> savedEntries,
        List<AttendanceCellError> errors
    ) { }

    // Settlement Period DTOs
    public record SettlementPeriodRequest(
        @NotBlank String periodCode,
        @NotBlank String startDate,
        @NotBlank String endDate,
        String cycleType
    ) { }

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
    ) { }

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
    ) { }

    public record SettlementIssueResponse(
        String id, String workerId, String workerName, String severity,
        String code, String message
    ) { }

    // --- Bulk Attendance Update DTOs ---
    public record BulkUpdateAttendanceRequest(
        @NotBlank String workDate,
        @NotNull List<@NotBlank String> workerIds,
        @NotNull BigDecimal attendanceValue,
        boolean overrideExisting
    ) { }

    public record BulkUpdateAttendanceResponse(
        int updatedCount
    ) { }

    // --- Calculation Rules DTOs ---
    public record CalculationRulesResponse(
        @NotNull BigDecimal overtimeRate,
        @NotNull BigDecimal overtimeThresholdHours,
        @NotNull BigDecimal deductionRatePerHour,
        @NotNull BigDecimal holidayPayRate,
        @NotNull String standardDailyHours,
        @NotBlank String description
    ) { }

    // Advance DTOs
    public record AdvanceRepayRequest(
        @NotNull BigDecimal amount,
        @NotBlank String repaymentType,    // PARTIAL or FULL
        String repaymentDate,
        String paymentMethod,
        String receiptRef,
        String notes
    ) { }

    public record AdvanceCreateRequest(
        @NotBlank String recipientType,
        String workerId,
        String contractorId,
        @NotNull BigDecimal amount,
        String termType,
        Integer totalInstallments,
        BigDecimal installmentAmount,
        String deductionFrequency,
        BigDecimal maxDeductionPercent,
        String reason,
        String firstInstallmentDate,
        String deductionMode,
        Integer deferralPeriods
    ) { }

    public record AdvanceResponse(
        String id,
        String recipientType,
        String workerId,
        String workerName,
        String contractorId,
        String contractorName,
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
    ) { }

    public record AdvancePolicyRequest(
        @NotBlank String scopeType,
        String scopeId,
        @NotBlank String deductionMode,
        @NotBlank String deductionFrequency,
        @NotNull BigDecimal maxDeductionPercent,
        int defaultInstallments,
        int deferralPeriods,
        boolean active,
        @NotBlank String effectiveFrom,
        String effectiveTo
    ) { }

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
        int version,
        String effectiveFrom,
        String effectiveTo,
        boolean active,
        long updatedAt
    ) { }
}
