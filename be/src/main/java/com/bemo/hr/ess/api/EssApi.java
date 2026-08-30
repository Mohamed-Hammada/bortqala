package com.bemo.hr.ess.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class EssApi {
    private EssApi() {
    }

    public record ProfileResponse(
            String employeeId,
            String employeeCode,
            String fullName,
            String categoryId,
            String categoryName,
            String employmentType,
            LocalDate activeFrom,
            BigDecimal baseSalary,
            BigDecimal annualLeaveRemainingDays,
            long pendingLeavesCount,
            long pendingAdvancesCount,
            int currentMonthPunchesCount,
            String lastPunchTime,
            String lastPunchType
    ) {
    }

    public record PayslipSummaryResponse(
            String paymentId,
            int periodYear,
            int periodMonth,
            String periodKind,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal grossTotal,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            String paymentStatus,
            Long paidAt
    ) {
    }

    public record PayslipDetailResponse(
            String paymentId,
            int periodYear,
            int periodMonth,
            String periodKind,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal baseSalary,
            BigDecimal grossTotal,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            String paymentStatus,
            Long paidAt,
            int scheduledDays,
            int attendedDays,
            int absentDays,
            BigDecimal overtimeHours,
            BigDecimal overtimeAmount,
            BigDecimal delayDeduction,
            BigDecimal absenceDeduction,
            BigDecimal advanceDeductions,
            BigDecimal bonusAmount,
            BigDecimal allowanceAmount,
            List<ExplanationItem> explanationItems
    ) {
    }

    public record ExplanationItem(
            String componentType,
            String label,
            BigDecimal amount,
            String calculationNote
    ) {
    }

    public record LeaveSubmitRequest(
            String leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {
    }

    public record LeaveResponse(
            String id,
            String requestNumber,
            String leaveTypeId,
            String leaveTypeName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalDays,
            String reason,
            String status,
            Long createdAt
    ) {
    }

    public record AdvanceSubmitRequest(
            BigDecimal amount,
            int totalInstallments,
            String firstInstallmentDate,
            String reason
    ) {
    }

    public record AdvanceResponse(
            String id,
            BigDecimal amount,
            int totalInstallments,
            BigDecimal installmentAmount,
            BigDecimal remainingBalance,
            String status,
            String firstInstallmentDate,
            String reason,
            Long createdAt
    ) {
    }

    public record AttendanceRecordResponse(
            LocalDate date,
            String checkIn,
            String checkOut,
            String status,
            BigDecimal hoursWorked
    ) {
    }
}
