package com.bemo.hr.workforce;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class ClientBillingApi {

    private ClientBillingApi() {
    }

    public record CreateRateRequest(
            @NotBlank String clientPartyId,
            @NotBlank String workerCategoryId,
            @NotNull @Positive BigDecimal dayRate,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String effectiveFrom,
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String effectiveTo
    ) {
    }

    public record RateResponse(
            String id,
            String clientPartyId,
            String workerCategoryId,
            String categoryName,
            BigDecimal dayRate,
            String effectiveFrom,
            String effectiveTo,
            long version
    ) {
    }

    public record GenerateBillingRequest(
            @NotBlank String clientPartyId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
    }

    public record BillingPeriodResponse(
            String id,
            String clientPartyId,
            String period,
            String status,
            String invoiceId,
            String invoiceNumber,
            BigDecimal totalAmount
    ) {
    }

    public record BillingLineResponse(
            String id,
            String workerId,
            String workerCode,
            String fullName,
            String categoryId,
            String categoryName,
            BigDecimal approvedDays,
            BigDecimal dayRate,
            BigDecimal amount,
            BigDecimal wageCost,
            BigDecimal varianceAmount,
            String lineStatus,
            String reason
    ) {
    }

    public record BillingReviewResponse(
            BillingPeriodResponse period,
            List<BillingLineResponse> lines,
            BigDecimal totalApprovedDays,
            BigDecimal totalBilledAmount,
            BigDecimal totalWageCost
    ) {
    }

    public record MarginRowResponse(
            String workerId,
            String workerCode,
            String fullName,
            String categoryName,
            BigDecimal approvedDays,
            BigDecimal dayRate,
            BigDecimal billedAmount,
            BigDecimal wageCost,
            BigDecimal marginAmount
    ) {
    }

    public record MarginReportResponse(
            String clientPartyId,
            String period,
            BigDecimal totalBilled,
            BigDecimal totalWageCost,
            BigDecimal totalMargin,
            List<MarginRowResponse> rows
    ) {
    }

    public record ConfirmResponse(
            String id,
            String clientPartyId,
            String period,
            String status,
            String invoiceId,
            String invoiceNumber,
            BigDecimal totalAmount
    ) {
    }
}