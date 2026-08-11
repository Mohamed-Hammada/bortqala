package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.domain.PaymentMethod;
import com.bemo.hr.payroll.domain.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PayrollApi {
    private PayrollApi() { }

    public record PayrollRow(
            String id,
            String employeeId,
            String employeeCode,
            String employeeName,
            String categoryId,
            String categoryName,
            String employmentType,
            String reportId,
            int periodYear,
            int periodMonth,
            String periodKind,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal baseSalary,
            BigDecimal attendanceBonus,
            BigDecimal attendanceDeduction,
            BigDecimal activeAdvancesBalance,
            BigDecimal grossAmount,
            BigDecimal advancesDeducted,
            BigDecimal otherDeductions,
            BigDecimal bonuses,
            BigDecimal netAmount,
            PaymentStatus paymentStatus,
            Instant paidAt,
            PaymentMethod paymentMethod,
            String referenceCode,
            String note,
            boolean incompleteProfile,
            String createdBy,
            Instant createdAt
    ) { }

    public record Summary(
            int totalEmployees,
            int paidCount,
            int pendingCount,
            BigDecimal totalGrossAmount,
            BigDecimal totalPaidAmount,
            BigDecimal totalPendingAmount,
            BigDecimal totalAdvancesDeducted
    ) { }

    public record ExplanationResponse(
        String id,
        String salaryPaymentId,
        String componentType,
        String formula,
        String inputValuesJson,
        BigDecimal calculatedAmount,
        String explanationTextAr,
        String explanationTextEn,
        long createdAt
    ) { }

    public record SheetResponse(
            int periodYear,
            int periodMonth,
            PaymentStatus periodStatus,
            Summary summary,
            List<PayrollRow> rows
    ) { }

    public record PaymentRequest(
            @NotBlank String employeeId,
            @NotNull @Min(2000) Integer periodYear,
            @NotNull @Min(1) Integer periodMonth,
            String periodKind,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal grossAmount,
            BigDecimal advancesDeducted,
            BigDecimal otherDeductions,
            BigDecimal bonuses,
            BigDecimal netAmount,
            PaymentMethod paymentMethod,
            String referenceCode,
            String note,
            Long paidAtEpochMs
    ) { }

    public record BulkPaymentRequest(
            @NotNull @Min(2000) Integer periodYear,
            @NotNull @Min(1) Integer periodMonth,
            String categoryId,
            PaymentMethod paymentMethod,
            String referenceCode,
            String note
    ) { }

    public record StatusTransitionRequest(
            @NotNull @Min(2000) Integer periodYear,
            @NotNull @Min(1) Integer periodMonth,
            @NotNull PaymentStatus targetStatus,
            String categoryId
    ) { }

    public record ReversePaymentRequest(
            @NotBlank String paymentId,
            @NotBlank String reason
    ) { }
}
