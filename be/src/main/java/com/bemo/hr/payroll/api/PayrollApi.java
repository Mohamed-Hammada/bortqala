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
    private PayrollApi() {
    }

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
            Instant createdAt,
            String paidBy,
            String reversedBy,
            Instant reversedAt,
            String reversalReason,
            long version
    ) {
    }

    public record Summary(
            int totalEmployees,
            int paidCount,
            int pendingCount,
            BigDecimal totalGrossAmount,
            BigDecimal totalPaidAmount,
            BigDecimal totalPendingAmount,
            BigDecimal totalAdvancesDeducted
    ) {
    }

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
    ) {
    }

    public record SheetResponse(
            int periodYear,
            int periodMonth,
            PaymentStatus periodStatus,
            Summary summary,
            List<PayrollRow> rows
    ) {
    }

    public record PaymentRequest(
            @NotBlank String employeeId,
            @NotNull @Min(2000) Integer periodYear,
            @NotNull @Min(1) Integer periodMonth,
            String periodKind,
            PaymentMethod paymentMethod,
            String referenceCode,
            String note,
            Long paidAtEpochMs,
            @NotNull Long expectedVersion
    ) {
    }

    public record BulkPaymentRequest(
            @NotNull @Min(2000) Integer periodYear,
            @NotNull @Min(1) Integer periodMonth,
            String categoryId,
            PaymentMethod paymentMethod,
            String referenceCode,
            String note
    ) {
    }

    public record StatusTransitionRequest(
            @NotNull @Min(2000) Integer periodYear,
            @NotNull @Min(1) Integer periodMonth,
            @NotNull PaymentStatus targetStatus
    ) {
    }

    public record ReversePaymentRequest(
            @NotBlank String paymentId,
            @NotBlank String reason,
            @NotNull Long expectedVersion
    ) {
    }

    public record CalculationPolicyRequest(
            @NotBlank String name,
            @NotNull Long effectiveFrom,
            Long effectiveTo,
            @NotNull BigDecimal workingHourDivisor,
            @NotNull BigDecimal overtimeMultiplier
    ) {
    }

    public record CalculationPolicyResponse(
            String id, String name, long effectiveFrom, Long effectiveTo,
            BigDecimal workingHourDivisor, BigDecimal overtimeMultiplier, long version
    ) {
    }

    public record StatutoryTaxRequest(
            @NotNull BigDecimal grossSalary
    ) {
    }

    public record StatutoryTaxBracketResponse(
            int bracketNumber,
            String bracketRange,
            BigDecimal ratePercent,
            BigDecimal taxableAmountInBracket,
            BigDecimal computedTax
    ) {
    }

    public record StatutoryTaxResponse(
            BigDecimal monthlyGrossSalary,
            BigDecimal monthlyInsurableWage,
            BigDecimal monthlyEmployeeSocialInsurance,
            BigDecimal monthlyEmployerSocialInsurance,
            BigDecimal monthlyMartyrsFund,
            BigDecimal annualTaxableIncome,
            BigDecimal annualIncomeTax,
            BigDecimal monthlyIncomeTax,
            BigDecimal totalEmployeeStatutoryDeductions,
            BigDecimal monthlyNetSalary,
            List<StatutoryTaxBracketResponse> taxBracketsBreakdown
    ) {
    }
}
