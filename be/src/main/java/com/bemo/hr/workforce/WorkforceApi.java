package com.bemo.hr.workforce;

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
        @NotNull List<AttendanceCell> entries
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
        BigDecimal netContractorsPayable
    ) { }

    // Advance DTOs
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
        String reason
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
        long createdAt
    ) { }
}
