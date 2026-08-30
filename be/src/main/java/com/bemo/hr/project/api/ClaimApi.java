package com.bemo.hr.project.api;

import com.bemo.hr.project.domain.AdjustmentType;
import com.bemo.hr.project.domain.ClaimKind;
import com.bemo.hr.project.domain.ClaimLineType;
import com.bemo.hr.project.domain.ClaimStatus;
import com.bemo.hr.project.domain.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ClaimApi {

    private ClaimApi() {
    }

    public record ProjectProgressClaimResponse(
            String id,
            String claimNumber,
            ClaimType claimType,
            ClaimKind claimKind,
            int claimSequenceNumber,
            String projectId,
            String partyId,
            String partyName,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            Long submissionDate,
            String currencyCode,
            BigDecimal previousGrossAmount,
            BigDecimal currentGrossAmount,
            BigDecimal cumulativeGrossAmount,
            BigDecimal previousRetentionAmount,
            BigDecimal currentRetentionAmount,
            BigDecimal cumulativeRetentionAmount,
            BigDecimal previousAdvanceRecoveryAmount,
            BigDecimal currentAdvanceRecoveryAmount,
            BigDecimal cumulativeAdvanceRecoveryAmount,
            BigDecimal currentTaxAmount,
            BigDecimal currentDeductionsAmount,
            BigDecimal currentNetPayableAmount,
            BigDecimal cumulativeNetPaidAmount,
            ClaimStatus status,
            String certifiedByUserId,
            Long certifiedAt,
            String certificationNotes,
            String postedFinanceJournalId,
            String postedInvoiceId,
            Long postedAt,
            String notes,
            int linesCount,
            long createdAt,
            long updatedAt,
            long version,
            List<ProgressClaimLineResponse> lines,
            List<ProgressClaimAdjustmentResponse> adjustments
    ) {}

    public record ProgressClaimLineResponse(
            String id,
            String claimId,
            ClaimLineType lineType,
            String wbsNodeId,
            String itemCode,
            String description,
            String unitOfMeasure,
            BigDecimal contractQuantity,
            BigDecimal unitRate,
            BigDecimal previousQuantity,
            BigDecimal currentQuantity,
            BigDecimal cumulativeQuantity,
            BigDecimal previousAmount,
            BigDecimal currentAmount,
            BigDecimal cumulativeAmount,
            BigDecimal percentComplete,
            String remarks,
            int sortOrder
    ) {}

    public record ProgressClaimAdjustmentResponse(
            String id,
            String claimId,
            AdjustmentType adjustmentType,
            String description,
            BigDecimal percentageRate,
            BigDecimal calculationBasisAmount,
            BigDecimal adjustmentAmount,
            boolean isAddition,
            String notes
    ) {}

    public record CreateProgressClaimRequest(
            @NotNull ClaimType claimType,
            @NotNull ClaimKind claimKind,
            @NotBlank String projectId,
            String partyId,
            @NotNull LocalDate periodStartDate,
            @NotNull LocalDate periodEndDate,
            String currencyCode,
            String notes,
            boolean initFromWbs
    ) {}

    public record UpdateProgressClaimRequest(
            @NotNull ClaimKind claimKind,
            String partyId,
            @NotNull LocalDate periodStartDate,
            @NotNull LocalDate periodEndDate,
            String currencyCode,
            String notes,
            List<SaveClaimLineRequest> lines,
            List<SaveClaimAdjustmentRequest> adjustments
    ) {}

    public record SaveClaimLineRequest(
            String id,
            ClaimLineType lineType,
            String wbsNodeId,
            @NotBlank String itemCode,
            @NotBlank String description,
            String unitOfMeasure,
            BigDecimal contractQuantity,
            @NotNull BigDecimal unitRate,
            BigDecimal previousQuantity,
            @NotNull BigDecimal currentQuantity,
            String remarks,
            int sortOrder
    ) {}

    public record SaveClaimAdjustmentRequest(
            String id,
            @NotNull AdjustmentType adjustmentType,
            String description,
            BigDecimal percentageRate,
            BigDecimal fixedAmount,
            boolean isAddition,
            String notes
    ) {}

    public record CertifyClaimRequest(
            String notes
    ) {}
}
