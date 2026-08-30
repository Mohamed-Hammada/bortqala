package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.TenderApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProjectTenderService {

    private final ProjectTenderRepository tenderRepository;
    private final TenderBoqItemRepository boqItemRepository;
    private final TenderBidderRepository bidderRepository;
    private final BidSubmissionLineRepository submissionLineRepository;
    private final TenderClarificationRepository clarificationRepository;
    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    public ProjectTenderService(
            ProjectTenderRepository tenderRepository,
            TenderBoqItemRepository boqItemRepository,
            TenderBidderRepository bidderRepository,
            BidSubmissionLineRepository submissionLineRepository,
            TenderClarificationRepository clarificationRepository,
            ProjectRepository projectRepository,
            AuditService auditService) {
        this.tenderRepository = tenderRepository;
        this.boqItemRepository = boqItemRepository;
        this.bidderRepository = bidderRepository;
        this.submissionLineRepository = submissionLineRepository;
        this.clarificationRepository = clarificationRepository;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProjectTenderResponse> listTenders() {
        return tenderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapTenderSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectTenderResponse getTender(String tenderId) {
        ProjectTender tender = requireTender(tenderId);
        List<TenderBoqItemResponse> boqItems = boqItemRepository.findByTenderIdOrderBySortOrderAsc(tenderId).stream()
                .map(this::mapBoqItemResponse).toList();
        List<TenderBidderResponse> bidders = bidderRepository.findByTenderIdOrderByRankOrderAsc(tenderId).stream()
                .map(b -> {
                    List<BidSubmissionLineResponse> lines = submissionLineRepository.findByBidderId(b.getId())
                            .stream().map(this::mapSubmissionLineResponse).toList();
                    return mapBidderResponse(b, lines);
                }).toList();
        List<TenderClarificationResponse> clarifications = clarificationRepository.findByTenderIdOrderByAskedAtDesc(tenderId).stream()
                .map(this::mapClarificationResponse).toList();

        String awardedBidderName = null;
        if (tender.getAwardedBidderId() != null) {
            awardedBidderName = bidderRepository.findById(tender.getAwardedBidderId())
                    .map(TenderBidder::getBidderName).orElse(null);
        }

        return new ProjectTenderResponse(
                tender.getId(),
                tender.getTenderNumber(),
                tender.getTitle(),
                tender.getTitleEn(),
                tender.getTenderType(),
                tender.getProjectId(),
                tender.getClientPartyId(),
                tender.getSubmissionDeadline(),
                tender.getEstimatedValue(),
                tender.getCurrencyCode(),
                tender.getTechnicalWeightPercent(),
                tender.getFinancialWeightPercent(),
                tender.isBidBondRequired(),
                tender.getBidBondAmount(),
                tender.getBidBondValidityDays(),
                tender.getStatus(),
                tender.getAwardedBidderId(),
                awardedBidderName,
                tender.getAwardedAmount(),
                tender.getAwardedAt(),
                tender.getNotes(),
                boqItems.size(),
                bidders.size(),
                tender.getCreatedAt(),
                tender.getUpdatedAt(),
                tender.getVersion(),
                boqItems,
                bidders,
                clarifications
        );
    }

    public ProjectTenderResponse createTender(CreateTenderRequest req, String userId) {
        String tenderNumber = generateTenderNumber();
        ProjectTender tender = new ProjectTender(
                tenderNumber,
                req.title(),
                req.titleEn(),
                req.tenderType(),
                req.projectId(),
                req.clientPartyId(),
                req.submissionDeadline(),
                req.estimatedValue(),
                req.currencyCode(),
                req.technicalWeightPercent(),
                req.financialWeightPercent(),
                req.bidBondRequired(),
                req.bidBondAmount(),
                req.bidBondValidityDays(),
                req.notes()
        );
        tender = tenderRepository.save(tender);

        auditService.record("TENDER_CREATE", "PROJECT_TENDER", tender.getId(), userId,
                "Created tender " + tender.getTenderNumber() + " - " + tender.getTitle(), null);

        return getTender(tender.getId());
    }

    public ProjectTenderResponse updateTender(String tenderId, UpdateTenderRequest req, String userId) {
        ProjectTender tender = requireTender(tenderId);
        tender.updateDraft(
                req.title(),
                req.titleEn(),
                req.tenderType(),
                req.projectId(),
                req.clientPartyId(),
                req.submissionDeadline(),
                req.estimatedValue(),
                req.currencyCode(),
                req.technicalWeightPercent(),
                req.financialWeightPercent(),
                req.bidBondRequired(),
                req.bidBondAmount(),
                req.bidBondValidityDays(),
                req.notes()
        );
        tender = tenderRepository.save(tender);

        auditService.record("TENDER_UPDATE", "PROJECT_TENDER", tender.getId(), userId,
                "Updated tender " + tender.getTenderNumber(), null);

        return getTender(tender.getId());
    }

    public void deleteTender(String tenderId, String userId) {
        ProjectTender tender = requireTender(tenderId);
        if (tender.getStatus() != TenderStatus.DRAFT) {
            throw new BusinessRuleException("CANNOT_DELETE_NON_DRAFT_TENDER");
        }

        List<TenderBidder> bidders = bidderRepository.findByTenderIdOrderByRankOrderAsc(tenderId);
        for (TenderBidder b : bidders) {
            submissionLineRepository.deleteByBidderId(b.getId());
        }
        bidderRepository.deleteByTenderId(tenderId);
        boqItemRepository.deleteByTenderId(tenderId);
        tenderRepository.delete(tender);

        auditService.record("TENDER_DELETE", "PROJECT_TENDER", tenderId, userId,
                "Deleted tender " + tender.getTenderNumber(), null);
    }

    public ProjectTenderResponse publishTender(String tenderId, String userId) {
        ProjectTender tender = requireTender(tenderId);
        tender.publish();
        tender = tenderRepository.save(tender);

        auditService.record("TENDER_PUBLISH", "PROJECT_TENDER", tender.getId(), userId,
                "Published tender " + tender.getTenderNumber(), null);

        return getTender(tender.getId());
    }

    public ProjectTenderResponse cancelTender(String tenderId, String userId) {
        ProjectTender tender = requireTender(tenderId);
        tender.cancel();
        tender = tenderRepository.save(tender);

        auditService.record("TENDER_CANCEL", "PROJECT_TENDER", tender.getId(), userId,
                "Cancelled tender " + tender.getTenderNumber(), null);

        return getTender(tender.getId());
    }

    // ─── BOQ Line Items ───────────────────────────────────────────────

    public TenderBoqItemResponse addBoqItem(String tenderId, CreateBoqItemRequest req, String userId) {
        ProjectTender tender = requireTender(tenderId);
        TenderBoqItem item = new TenderBoqItem(
                tenderId,
                req.itemCode(),
                req.description(),
                req.descriptionEn(),
                req.unitOfMeasure(),
                req.quantity(),
                req.estimatedRate(),
                req.sortOrder()
        );
        item = boqItemRepository.save(item);

        recalculateTenderEstimatedValue(tender);

        auditService.record("TENDER_BOQ_ADD", "TENDER_BOQ_ITEM", item.getId(), userId,
                "Added BOQ item " + item.getItemCode() + " to tender " + tender.getTenderNumber(), null);

        return mapBoqItemResponse(item);
    }

    public TenderBoqItemResponse updateBoqItem(String tenderId, String itemId, UpdateBoqItemRequest req, String userId) {
        ProjectTender tender = requireTender(tenderId);
        TenderBoqItem item = requireBoqItem(itemId);

        item.update(
                req.itemCode(),
                req.description(),
                req.descriptionEn(),
                req.unitOfMeasure(),
                req.quantity(),
                req.estimatedRate(),
                req.sortOrder()
        );
        item = boqItemRepository.save(item);

        recalculateTenderEstimatedValue(tender);

        auditService.record("TENDER_BOQ_UPDATE", "TENDER_BOQ_ITEM", item.getId(), userId,
                "Updated BOQ item " + item.getItemCode(), null);

        return mapBoqItemResponse(item);
    }

    public void deleteBoqItem(String tenderId, String itemId, String userId) {
        ProjectTender tender = requireTender(tenderId);
        TenderBoqItem item = requireBoqItem(itemId);
        boqItemRepository.delete(item);

        recalculateTenderEstimatedValue(tender);

        auditService.record("TENDER_BOQ_DELETE", "TENDER_BOQ_ITEM", itemId, userId,
                "Deleted BOQ item " + item.getItemCode(), null);
    }

    // ─── Bidder Management & Submissions ─────────────────────────────

    public TenderBidderResponse inviteBidder(String tenderId, InviteBidderRequest req, String userId) {
        requireTender(tenderId);
        TenderBidder bidder = new TenderBidder(
                tenderId,
                req.partyId(),
                req.bidderName(),
                req.contactEmail(),
                req.contactPhone(),
                req.notes()
        );
        bidder = bidderRepository.save(bidder);

        auditService.record("TENDER_BIDDER_INVITE", "TENDER_BIDDER", bidder.getId(), userId,
                "Invited bidder " + bidder.getBidderName() + " to tender", null);

        return mapBidderResponse(bidder, List.of());
    }

    public TenderBidderResponse submitBid(String tenderId, String bidderId, SubmitBidRequest req, String userId) {
        requireTender(tenderId);
        TenderBidder bidder = requireBidder(bidderId);

        submissionLineRepository.deleteByBidderId(bidderId);

        Map<String, TenderBoqItem> boqMap = boqItemRepository.findByTenderIdOrderBySortOrderAsc(tenderId).stream()
                .collect(Collectors.toMap(TenderBoqItem::getId, b -> b));

        BigDecimal totalSum = BigDecimal.ZERO;
        List<BidSubmissionLine> createdLines = new ArrayList<>();

        for (BidLineSubmission line : req.lines()) {
            TenderBoqItem boqItem = boqMap.get(line.boqItemId());
            BigDecimal qty = boqItem != null ? boqItem.getQuantity() : BigDecimal.ONE;

            BidSubmissionLine subLine = new BidSubmissionLine(
                    bidderId,
                    line.boqItemId(),
                    line.unitRate(),
                    qty,
                    line.technicalRemarks(),
                    line.deviationsNotes()
            );
            subLine = submissionLineRepository.save(subLine);
            createdLines.add(subLine);
            totalSum = totalSum.add(subLine.getTotalAmount());
        }

        bidder.recordSubmission(totalSum);
        bidder = bidderRepository.save(bidder);

        auditService.record("TENDER_BID_SUBMIT", "TENDER_BIDDER", bidder.getId(), userId,
                "Submitted bid for bidder " + bidder.getBidderName() + " total=" + totalSum, null);

        List<BidSubmissionLineResponse> lineResponses = createdLines.stream()
                .map(this::mapSubmissionLineResponse).toList();
        return mapBidderResponse(bidder, lineResponses);
    }

    public TenderBidderResponse recordBidBond(String tenderId, String bidderId, RecordBidBondRequest req, String userId) {
        requireTender(tenderId);
        TenderBidder bidder = requireBidder(bidderId);

        LocalDate expiry = req.expiryDate() != null ?
                Instant.ofEpochMilli(req.expiryDate()).atZone(ZoneOffset.UTC).toLocalDate() : null;

        bidder.updateBidBond(req.received(), req.bondNumber(), expiry);
        bidder = bidderRepository.save(bidder);

        auditService.record("TENDER_BIDBOND_RECORD", "TENDER_BIDDER", bidder.getId(), userId,
                "Recorded bid bond for " + bidder.getBidderName(), null);

        List<BidSubmissionLineResponse> lines = submissionLineRepository.findByBidderId(bidder.getId())
                .stream().map(this::mapSubmissionLineResponse).toList();
        return mapBidderResponse(bidder, lines);
    }

    public void evaluateBidderTechnical(String tenderId, String bidderId, TechnicalEvaluationRequest req, String userId) {
        requireTender(tenderId);
        TenderBidder bidder = requireBidder(bidderId);
        bidder.recordEvaluation(req.technicalScore(), bidder.getFinancialScore(), bidder.getCombinedScore(),
                bidder.getRankOrder() != null ? bidder.getRankOrder() : 0);
        bidderRepository.save(bidder);

        auditService.record("TENDER_TECH_EVAL", "TENDER_BIDDER", bidderId, userId,
                "Evaluated technical score=" + req.technicalScore() + " for " + bidder.getBidderName(), null);
    }

    // ─── Deterministic Technical & Financial Evaluation Engine ─────────

    public TenderEvaluationSummaryResponse calculateTenderEvaluation(String tenderId, String userId) {
        ProjectTender tender = requireTender(tenderId);
        List<TenderBidder> bidders = bidderRepository.findByTenderIdOrderByRankOrderAsc(tenderId);

        List<TenderBidder> compliantBidders = bidders.stream()
                .filter(b -> b.getStatus() == BidderStatus.SUBMITTED && b.getTotalBidAmount() != null &&
                             b.getTotalBidAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (compliantBidders.isEmpty()) {
            throw new BusinessRuleException("NO_COMPLIANT_BIDS_TO_EVALUATE");
        }

        BigDecimal lowestBid = compliantBidders.stream()
                .map(TenderBidder::getTotalBidAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal techWeight = BigDecimal.valueOf(tender.getTechnicalWeightPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal finWeight = BigDecimal.valueOf(tender.getFinancialWeightPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        for (TenderBidder b : compliantBidders) {
            BigDecimal techScore = b.getTechnicalScore() != null ? b.getTechnicalScore() : BigDecimal.valueOf(70.0);

            // Financial Score = (LowestBid / BidderBid) * 100
            BigDecimal finScore = lowestBid.multiply(BigDecimal.valueOf(100))
                    .divide(b.getTotalBidAmount(), 2, RoundingMode.HALF_UP);

            // Combined Weighted Score = (TechScore * TechWeight) + (FinScore * FinWeight)
            BigDecimal combined = techScore.multiply(techWeight).add(finScore.multiply(finWeight))
                    .setScale(2, RoundingMode.HALF_UP);

            b.recordEvaluation(techScore, finScore, combined, 0);
        }

        // Rank by combined score descending
        List<TenderBidder> sorted = new ArrayList<>(compliantBidders);
        sorted.sort((a, b) -> b.getCombinedScore().compareTo(a.getCombinedScore()));

        for (int i = 0; i < sorted.size(); i++) {
            TenderBidder b = sorted.get(i);
            b.recordEvaluation(b.getTechnicalScore(), b.getFinancialScore(), b.getCombinedScore(), i + 1);
            bidderRepository.save(b);
        }

        if (tender.getStatus() == TenderStatus.PUBLISHED) {
            tender.startEvaluation();
            tenderRepository.save(tender);
        }

        auditService.record("TENDER_EVAL_CALCULATE", "PROJECT_TENDER", tender.getId(), userId,
                "Calculated evaluation scores for " + sorted.size() + " bidders", null);

        List<TenderBidderResponse> evaluatedResponses = sorted.stream().map(b -> {
            List<BidSubmissionLineResponse> lines = submissionLineRepository.findByBidderId(b.getId())
                    .stream().map(this::mapSubmissionLineResponse).toList();
            return mapBidderResponse(b, lines);
        }).toList();

        return new TenderEvaluationSummaryResponse(
                tender.getId(),
                tender.getTenderNumber(),
                lowestBid,
                tender.getTechnicalWeightPercent(),
                tender.getFinancialWeightPercent(),
                evaluatedResponses
        );
    }

    // ─── Awarding & Project Contract Linkage ───────────────────────────

    public ProjectTenderResponse awardTender(String tenderId, AwardTenderRequest req, String userId) {
        ProjectTender tender = requireTender(tenderId);
        TenderBidder winner = requireBidder(req.awardedBidderId());

        tender.award(winner.getId(), winner.getTotalBidAmount());
        ProjectTender savedTender = tenderRepository.save(tender);
        final String tenderNum = savedTender.getTenderNumber();

        if (req.updateProjectContract() && savedTender.getProjectId() != null) {
            projectRepository.findById(savedTender.getProjectId()).ifPresent(p -> {
                p.update(
                        p.getName(),
                        p.getNameEn(),
                        p.getDescription(),
                        p.getCompanyId(),
                        p.getBranchId(),
                        winner.getPartyId() != null ? winner.getPartyId() : p.getOwnerPartyId(),
                        p.getProjectManagerId(),
                        p.getSiteAddress(),
                        p.getContractNumber() != null ? p.getContractNumber() : tenderNum,
                        winner.getTotalBidAmount(),
                        p.getCurrencyCode(),
                        p.getStartDate(),
                        p.getEndDate(),
                        p.isBudgetBlocking()
                );
                projectRepository.save(p);
            });
        }

        auditService.record("TENDER_AWARD", "PROJECT_TENDER", savedTender.getId(), userId,
                "Awarded tender " + savedTender.getTenderNumber() + " to " + winner.getBidderName() +
                " for amount=" + winner.getTotalBidAmount(), null);

        return getTender(savedTender.getId());
    }

    // ─── Clarifications & Addenda ─────────────────────────────────────

    public TenderClarificationResponse addClarification(String tenderId, CreateClarificationRequest req, String userId) {
        requireTender(tenderId);
        TenderClarification clarif = new TenderClarification(
                tenderId,
                req.question(),
                req.askedByPartyId(),
                req.isPublicAddendum()
        );
        clarif = clarificationRepository.save(clarif);

        auditService.record("TENDER_CLARIF_ADD", "TENDER_CLARIFICATION", clarif.getId(), userId,
                "Added clarification question to tender", null);

        return mapClarificationResponse(clarif);
    }

    public TenderClarificationResponse answerClarification(String tenderId, String clarifId, AnswerClarificationRequest req, String userId) {
        requireTender(tenderId);
        TenderClarification clarif = clarificationRepository.findById(clarifId)
                .orElseThrow(() -> new NotFoundException("CLARIFICATION_NOT_FOUND"));

        clarif.provideAnswer(req.answer(), userId, req.isPublicAddendum());
        clarif = clarificationRepository.save(clarif);

        auditService.record("TENDER_CLARIF_ANSWER", "TENDER_CLARIFICATION", clarifId, userId,
                "Answered clarification on tender", null);

        return mapClarificationResponse(clarif);
    }

    // ─── Private Helpers ──────────────────────────────────────────────

    private void recalculateTenderEstimatedValue(ProjectTender tender) {
        List<TenderBoqItem> items = boqItemRepository.findByTenderIdOrderBySortOrderAsc(tender.getId());
        BigDecimal total = items.stream()
                .map(TenderBoqItem::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        tender.updateEstimatedValue(total);
        tenderRepository.save(tender);
    }

    private String generateTenderNumber() {
        int year = LocalDate.now().getYear();
        long seq = tenderRepository.countByYearPrefix(String.valueOf(year)) + 1;
        return String.format("TND-%d-%03d", year, seq);
    }

    private ProjectTenderResponse mapTenderSummaryResponse(ProjectTender t) {
        String awardedBidderName = null;
        if (t.getAwardedBidderId() != null) {
            awardedBidderName = bidderRepository.findById(t.getAwardedBidderId())
                    .map(TenderBidder::getBidderName).orElse(null);
        }
        int boqCount = boqItemRepository.findByTenderIdOrderBySortOrderAsc(t.getId()).size();
        int bidderCount = bidderRepository.findByTenderIdOrderByRankOrderAsc(t.getId()).size();

        return new ProjectTenderResponse(
                t.getId(),
                t.getTenderNumber(),
                t.getTitle(),
                t.getTitleEn(),
                t.getTenderType(),
                t.getProjectId(),
                t.getClientPartyId(),
                t.getSubmissionDeadline(),
                t.getEstimatedValue(),
                t.getCurrencyCode(),
                t.getTechnicalWeightPercent(),
                t.getFinancialWeightPercent(),
                t.isBidBondRequired(),
                t.getBidBondAmount(),
                t.getBidBondValidityDays(),
                t.getStatus(),
                t.getAwardedBidderId(),
                awardedBidderName,
                t.getAwardedAmount(),
                t.getAwardedAt(),
                t.getNotes(),
                boqCount,
                bidderCount,
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getVersion(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private TenderBoqItemResponse mapBoqItemResponse(TenderBoqItem i) {
        return new TenderBoqItemResponse(
                i.getId(),
                i.getTenderId(),
                i.getItemCode(),
                i.getDescription(),
                i.getDescriptionEn(),
                i.getUnitOfMeasure(),
                i.getQuantity(),
                i.getEstimatedRate(),
                i.getEstimatedAmount(),
                i.getSortOrder()
        );
    }

    private TenderBidderResponse mapBidderResponse(TenderBidder b, List<BidSubmissionLineResponse> lines) {
        Long bondExpiry = b.getBidBondExpiryDate() != null ?
                b.getBidBondExpiryDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() : null;

        return new TenderBidderResponse(
                b.getId(),
                b.getTenderId(),
                b.getPartyId(),
                b.getBidderName(),
                b.getContactEmail(),
                b.getContactPhone(),
                b.getStatus(),
                b.getInvitationDate(),
                b.getSubmissionDate(),
                b.getTechnicalScore(),
                b.getFinancialScore(),
                b.getCombinedScore(),
                b.getRankOrder(),
                b.getTotalBidAmount(),
                b.isBidBondReceived(),
                b.getBidBondNumber(),
                bondExpiry,
                b.getNotes(),
                lines
        );
    }

    private BidSubmissionLineResponse mapSubmissionLineResponse(BidSubmissionLine l) {
        return new BidSubmissionLineResponse(
                l.getId(),
                l.getBidderId(),
                l.getBoqItemId(),
                l.getUnitRate(),
                l.getTotalAmount(),
                l.getTechnicalRemarks(),
                l.getDeviationsNotes()
        );
    }

    private TenderClarificationResponse mapClarificationResponse(TenderClarification c) {
        return new TenderClarificationResponse(
                c.getId(),
                c.getTenderId(),
                c.getQuestion(),
                c.getAskedByPartyId(),
                c.getAskedAt(),
                c.getAnswer(),
                c.getAnsweredByUserId(),
                c.getAnsweredAt(),
                c.isPublicAddendum(),
                c.getCreatedAt()
        );
    }

    private ProjectTender requireTender(String tenderId) {
        return tenderRepository.findById(tenderId)
                .orElseThrow(() -> new NotFoundException("TENDER_NOT_FOUND"));
    }

    private TenderBoqItem requireBoqItem(String itemId) {
        return boqItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("BOQ_ITEM_NOT_FOUND"));
    }

    private TenderBidder requireBidder(String bidderId) {
        return bidderRepository.findById(bidderId)
                .orElseThrow(() -> new NotFoundException("BIDDER_NOT_FOUND"));
    }
}
