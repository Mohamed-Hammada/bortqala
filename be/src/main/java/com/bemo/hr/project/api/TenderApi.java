package com.bemo.hr.project.api;

import com.bemo.hr.project.domain.BidderStatus;
import com.bemo.hr.project.domain.TenderStatus;
import com.bemo.hr.project.domain.TenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class TenderApi {

    private TenderApi() {
    }

    public record ProjectTenderResponse(
            String id,
            String tenderNumber,
            String title,
            String titleEn,
            TenderType tenderType,
            String projectId,
            String clientPartyId,
            long submissionDeadline,
            BigDecimal estimatedValue,
            String currencyCode,
            int technicalWeightPercent,
            int financialWeightPercent,
            boolean bidBondRequired,
            BigDecimal bidBondAmount,
            Integer bidBondValidityDays,
            TenderStatus status,
            String awardedBidderId,
            String awardedBidderName,
            BigDecimal awardedAmount,
            Long awardedAt,
            String notes,
            int boqItemsCount,
            int biddersCount,
            long createdAt,
            long updatedAt,
            long version,
            List<TenderBoqItemResponse> boqItems,
            List<TenderBidderResponse> bidders,
            List<TenderClarificationResponse> clarifications
    ) {}

    public record TenderBoqItemResponse(
            String id,
            String tenderId,
            String itemCode,
            String description,
            String descriptionEn,
            String unitOfMeasure,
            BigDecimal quantity,
            BigDecimal estimatedRate,
            BigDecimal estimatedAmount,
            int sortOrder
    ) {}

    public record TenderBidderResponse(
            String id,
            String tenderId,
            String partyId,
            String bidderName,
            String contactEmail,
            String contactPhone,
            BidderStatus status,
            Long invitationDate,
            Long submissionDate,
            BigDecimal technicalScore,
            BigDecimal financialScore,
            BigDecimal combinedScore,
            Integer rankOrder,
            BigDecimal totalBidAmount,
            boolean bidBondReceived,
            String bidBondNumber,
            Long bidBondExpiryDate,
            String notes,
            List<BidSubmissionLineResponse> submissionLines
    ) {}

    public record BidSubmissionLineResponse(
            String id,
            String bidderId,
            String boqItemId,
            BigDecimal unitRate,
            BigDecimal totalAmount,
            String technicalRemarks,
            String deviationsNotes
    ) {}

    public record TenderClarificationResponse(
            String id,
            String tenderId,
            String question,
            String askedByPartyId,
            long askedAt,
            String answer,
            String answeredByUserId,
            Long answeredAt,
            boolean isPublicAddendum,
            long createdAt
    ) {}

    public record TenderEvaluationSummaryResponse(
            String tenderId,
            String tenderNumber,
            BigDecimal lowestCompliantBidAmount,
            int technicalWeightPercent,
            int financialWeightPercent,
            List<TenderBidderResponse> evaluatedBidders
    ) {}

    public record CreateTenderRequest(
            @NotBlank String title,
            String titleEn,
            @NotNull TenderType tenderType,
            String projectId,
            String clientPartyId,
            @NotNull Long submissionDeadline,
            BigDecimal estimatedValue,
            String currencyCode,
            int technicalWeightPercent,
            int financialWeightPercent,
            boolean bidBondRequired,
            BigDecimal bidBondAmount,
            Integer bidBondValidityDays,
            String notes
    ) {}

    public record UpdateTenderRequest(
            @NotBlank String title,
            String titleEn,
            @NotNull TenderType tenderType,
            String projectId,
            String clientPartyId,
            @NotNull Long submissionDeadline,
            BigDecimal estimatedValue,
            String currencyCode,
            int technicalWeightPercent,
            int financialWeightPercent,
            boolean bidBondRequired,
            BigDecimal bidBondAmount,
            Integer bidBondValidityDays,
            String notes
    ) {}

    public record CreateBoqItemRequest(
            @NotBlank String itemCode,
            @NotBlank String description,
            String descriptionEn,
            @NotBlank String unitOfMeasure,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal estimatedRate,
            int sortOrder
    ) {}

    public record UpdateBoqItemRequest(
            @NotBlank String itemCode,
            @NotBlank String description,
            String descriptionEn,
            @NotBlank String unitOfMeasure,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal estimatedRate,
            int sortOrder
    ) {}

    public record InviteBidderRequest(
            String partyId,
            @NotBlank String bidderName,
            String contactEmail,
            String contactPhone,
            String notes
    ) {}

    public record BidLineSubmission(
            @NotBlank String boqItemId,
            @NotNull BigDecimal unitRate,
            String technicalRemarks,
            String deviationsNotes
    ) {}

    public record SubmitBidRequest(
            @NotNull List<BidLineSubmission> lines,
            String notes
    ) {}

    public record TechnicalEvaluationRequest(
            @NotNull BigDecimal technicalScore,
            String remarks
    ) {}

    public record RecordBidBondRequest(
            boolean received,
            String bondNumber,
            Long expiryDate
    ) {}

    public record CreateClarificationRequest(
            @NotBlank String question,
            String askedByPartyId,
            boolean isPublicAddendum
    ) {}

    public record AnswerClarificationRequest(
            @NotBlank String answer,
            boolean isPublicAddendum
    ) {}

    public record AwardTenderRequest(
            @NotBlank String awardedBidderId,
            String notes,
            boolean updateProjectContract
    ) {}
}
