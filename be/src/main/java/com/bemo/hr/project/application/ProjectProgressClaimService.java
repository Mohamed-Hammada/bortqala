package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.project.api.ClaimApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProjectProgressClaimService {

    private final ProjectProgressClaimRepository claimRepository;
    private final ProgressClaimLineRepository lineRepository;
    private final ProgressClaimAdjustmentRepository adjustmentRepository;
    private final ProjectRepository projectRepository;
    private final WbsNodeRepository wbsNodeRepository;
    private final BusinessPartyRepository partyRepository;
    private final AuditService auditService;

    public ProjectProgressClaimService(
            ProjectProgressClaimRepository claimRepository,
            ProgressClaimLineRepository lineRepository,
            ProgressClaimAdjustmentRepository adjustmentRepository,
            ProjectRepository projectRepository,
            WbsNodeRepository wbsNodeRepository,
            BusinessPartyRepository partyRepository,
            AuditService auditService) {
        this.claimRepository = claimRepository;
        this.lineRepository = lineRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.projectRepository = projectRepository;
        this.wbsNodeRepository = wbsNodeRepository;
        this.partyRepository = partyRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProjectProgressClaimResponse> listClaimsForProject(String projectId) {
        return claimRepository.findByProjectIdOrderByClaimSequenceNumberDesc(projectId).stream()
                .map(this::mapClaimSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectProgressClaimResponse getClaim(String claimId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        List<ProgressClaimLineResponse> lines = lineRepository.findByClaimIdOrderBySortOrderAsc(claimId).stream()
                .map(this::mapLineResponse).toList();
        List<ProgressClaimAdjustmentResponse> adjustments = adjustmentRepository.findByClaimId(claimId).stream()
                .map(this::mapAdjustmentResponse).toList();

        String partyName = null;
        if (claim.getPartyId() != null) {
            partyName = partyRepository.findById(claim.getPartyId())
                    .map(BusinessParty::getName).orElse(null);
        }

        return new ProjectProgressClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getClaimType(),
                claim.getClaimKind(),
                claim.getClaimSequenceNumber(),
                claim.getProjectId(),
                claim.getPartyId(),
                partyName,
                claim.getPeriodStartDate(),
                claim.getPeriodEndDate(),
                claim.getSubmissionDate(),
                claim.getCurrencyCode(),
                claim.getPreviousGrossAmount(),
                claim.getCurrentGrossAmount(),
                claim.getCumulativeGrossAmount(),
                claim.getPreviousRetentionAmount(),
                claim.getCurrentRetentionAmount(),
                claim.getCumulativeRetentionAmount(),
                claim.getPreviousAdvanceRecoveryAmount(),
                claim.getCurrentAdvanceRecoveryAmount(),
                claim.getCumulativeAdvanceRecoveryAmount(),
                claim.getCurrentTaxAmount(),
                claim.getCurrentDeductionsAmount(),
                claim.getCurrentNetPayableAmount(),
                claim.getCumulativeNetPaidAmount(),
                claim.getStatus(),
                claim.getCertifiedByUserId(),
                claim.getCertifiedAt(),
                claim.getCertificationNotes(),
                claim.getPostedFinanceJournalId(),
                claim.getPostedInvoiceId(),
                claim.getPostedAt(),
                claim.getNotes(),
                lines.size(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                claim.getVersion(),
                lines,
                adjustments
        );
    }

    public ProjectProgressClaimResponse createClaim(CreateProgressClaimRequest req, String userId) {
        Project project = projectRepository.findById(req.projectId())
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND"));

        List<ProjectProgressClaim> existing = claimRepository
                .findByProjectIdAndClaimTypeOrderByClaimSequenceNumberDesc(req.projectId(), req.claimType());
        int nextSeq = existing.isEmpty() ? 1 : existing.get(0).getClaimSequenceNumber() + 1;

        String claimNumber = generateClaimNumber(req.claimType());

        ProjectProgressClaim claim = new ProjectProgressClaim(
                claimNumber,
                req.claimType(),
                req.claimKind(),
                nextSeq,
                req.projectId(),
                req.partyId() != null ? req.partyId() : project.getOwnerPartyId(),
                req.periodStartDate(),
                req.periodEndDate(),
                req.currencyCode() != null ? req.currencyCode() : project.getCurrencyCode(),
                req.notes()
        );
        claim = claimRepository.save(claim);

        // Populate previous values from prior approved claim
        Optional<ProjectProgressClaim> priorClaimOpt = claimRepository
                .findPreviousApprovedClaim(req.projectId(), req.claimType(), nextSeq);

        Map<String, BigDecimal> prevQtyByWbs = new HashMap<>();
        if (priorClaimOpt.isPresent()) {
            ProjectProgressClaim prior = priorClaimOpt.get();
            List<ProgressClaimLine> priorLines = lineRepository.findByClaimIdOrderBySortOrderAsc(prior.getId());
            for (ProgressClaimLine pl : priorLines) {
                if (pl.getWbsNodeId() != null) {
                    prevQtyByWbs.put(pl.getWbsNodeId(), pl.getCumulativeQuantity());
                }
            }
        }

        // Initialize default lines from Project WBS if requested
        if (req.initFromWbs()) {
            List<WbsNode> wbsNodes = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(req.projectId());
            int sort = 1;
            for (WbsNode node : wbsNodes) {
                if (node.getNodeType() == WbsNodeType.BOQ_ITEM || node.getNodeType() == WbsNodeType.WORK_PACKAGE) {
                    BigDecimal prevQty = prevQtyByWbs.getOrDefault(node.getId(), BigDecimal.ZERO);
                    ProgressClaimLine line = new ProgressClaimLine(
                            claim.getId(),
                            ClaimLineType.BOQ_ITEM,
                            node.getId(),
                            node.getWbsCode(),
                            node.getName(),
                            node.getUnitOfMeasure() != null ? node.getUnitOfMeasure() : "PCS",
                            node.getPlannedQuantity(),
                            node.getUnitRate(),
                            prevQty,
                            BigDecimal.ZERO,
                            null,
                            sort++
                    );
                    lineRepository.save(line);
                }
            }

            // Create default retention & advance recovery adjustments
            ProgressClaimAdjustment retentionAdj = new ProgressClaimAdjustment(
                    claim.getId(),
                    AdjustmentType.RETENTION,
                    "تأمين أعمال وضمان (5%)",
                    BigDecimal.valueOf(5.0),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    "Standard 5% retention"
            );
            adjustmentRepository.save(retentionAdj);

            recalculateClaimFinancialTotals(claim);
        }

        auditService.record("CLAIM_CREATE", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Created claim " + claim.getClaimNumber() + " for project " + project.getName(), null);

        return getClaim(claim.getId());
    }

    public ProjectProgressClaimResponse updateDraftClaim(String claimId, UpdateProgressClaimRequest req, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        claim.updateDraft(
                req.claimKind(),
                req.partyId(),
                req.periodStartDate(),
                req.periodEndDate(),
                req.currencyCode(),
                req.notes()
        );

        if (req.lines() != null) {
            lineRepository.deleteByClaimId(claimId);
            int sort = 1;
            for (SaveClaimLineRequest l : req.lines()) {
                ProgressClaimLine line = new ProgressClaimLine(
                        claimId,
                        l.lineType() != null ? l.lineType() : ClaimLineType.BOQ_ITEM,
                        l.wbsNodeId(),
                        l.itemCode(),
                        l.description(),
                        l.unitOfMeasure(),
                        l.contractQuantity(),
                        l.unitRate(),
                        l.previousQuantity() != null ? l.previousQuantity() : BigDecimal.ZERO,
                        l.currentQuantity() != null ? l.currentQuantity() : BigDecimal.ZERO,
                        l.remarks(),
                        l.sortOrder() > 0 ? l.sortOrder() : sort++
                );
                lineRepository.save(line);
            }
        }

        if (req.adjustments() != null) {
            adjustmentRepository.deleteByClaimId(claimId);
            for (SaveClaimAdjustmentRequest a : req.adjustments()) {
                ProgressClaimAdjustment adj = new ProgressClaimAdjustment(
                        claimId,
                        a.adjustmentType(),
                        a.description(),
                        a.percentageRate(),
                        claim.getCurrentGrossAmount(),
                        a.fixedAmount(),
                        a.isAddition(),
                        a.notes()
                );
                adjustmentRepository.save(adj);
            }
        }

        recalculateClaimFinancialTotals(claim);

        auditService.record("CLAIM_UPDATE", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Updated claim " + claim.getClaimNumber(), null);

        return getClaim(claim.getId());
    }

    public void deleteDraftClaim(String claimId, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new BusinessRuleException("CANNOT_DELETE_NON_DRAFT_CLAIM");
        }

        lineRepository.deleteByClaimId(claimId);
        adjustmentRepository.deleteByClaimId(claimId);
        claimRepository.delete(claim);

        auditService.record("CLAIM_DELETE", "PROJECT_PROGRESS_CLAIM", claimId, userId,
                "Deleted claim " + claim.getClaimNumber(), null);
    }

    // ─── Lifecycle Transitions ────────────────────────────────────────

    public ProjectProgressClaimResponse submitClaim(String claimId, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        claim.submit();
        claim = claimRepository.save(claim);

        auditService.record("CLAIM_SUBMIT", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Submitted claim " + claim.getClaimNumber(), null);

        return getClaim(claim.getId());
    }

    public ProjectProgressClaimResponse reviewClaim(String claimId, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        claim.review();
        claim = claimRepository.save(claim);

        auditService.record("CLAIM_REVIEW", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Reviewed claim " + claim.getClaimNumber(), null);

        return getClaim(claim.getId());
    }

    public ProjectProgressClaimResponse certifyClaim(String claimId, CertifyClaimRequest req, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        claim.certify(userId, req != null ? req.notes() : null);
        claim = claimRepository.save(claim);

        auditService.record("CLAIM_CERTIFY", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Certified claim " + claim.getClaimNumber() + " with net payable=" + claim.getCurrentNetPayableAmount(), null);

        return getClaim(claim.getId());
    }

    public ProjectProgressClaimResponse postClaimToFinance(String claimId, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        String generatedJournalId = UUID.randomUUID().toString();
        String generatedInvoiceId = "INV-" + claim.getClaimNumber();

        claim.markPostedFinance(generatedJournalId, generatedInvoiceId);
        claim = claimRepository.save(claim);

        auditService.record("CLAIM_POST_FINANCE", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Posted claim " + claim.getClaimNumber() + " to finance journal=" + generatedJournalId, null);

        return getClaim(claim.getId());
    }

    public ProjectProgressClaimResponse cancelClaim(String claimId, String userId) {
        ProjectProgressClaim claim = requireClaim(claimId);
        claim.cancel();
        claim = claimRepository.save(claim);

        auditService.record("CLAIM_CANCEL", "PROJECT_PROGRESS_CLAIM", claim.getId(), userId,
                "Cancelled claim " + claim.getClaimNumber(), null);

        return getClaim(claim.getId());
    }

    // ─── Calculation & Rollup Engine ──────────────────────────────────

    private void recalculateClaimFinancialTotals(ProjectProgressClaim claim) {
        List<ProgressClaimLine> lines = lineRepository.findByClaimIdOrderBySortOrderAsc(claim.getId());

        BigDecimal prevGross = BigDecimal.ZERO;
        BigDecimal currGross = BigDecimal.ZERO;
        BigDecimal cumGross = BigDecimal.ZERO;

        for (ProgressClaimLine l : lines) {
            prevGross = prevGross.add(l.getPreviousAmount());
            currGross = currGross.add(l.getCurrentAmount());
            cumGross = cumGross.add(l.getCumulativeAmount());
        }

        List<ProgressClaimAdjustment> adjustments = adjustmentRepository.findByClaimId(claim.getId());

        BigDecimal currRetention = BigDecimal.ZERO;
        BigDecimal currAdvance = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal deductionsAmount = BigDecimal.ZERO;

        for (ProgressClaimAdjustment adj : adjustments) {
            adj.recalculate(currGross);
            adjustmentRepository.save(adj);

            if (adj.getAdjustmentType() == AdjustmentType.RETENTION) {
                currRetention = currRetention.add(adj.getAdjustmentAmount());
            } else if (adj.getAdjustmentType() == AdjustmentType.ADVANCE_RECOVERY) {
                currAdvance = currAdvance.add(adj.getAdjustmentAmount());
            } else if (adj.isAddition()) {
                taxAmount = taxAmount.add(adj.getAdjustmentAmount());
            } else {
                deductionsAmount = deductionsAmount.add(adj.getAdjustmentAmount());
            }
        }

        Optional<ProjectProgressClaim> priorOpt = claimRepository.findPreviousApprovedClaim(
                claim.getProjectId(), claim.getClaimType(), claim.getClaimSequenceNumber());

        BigDecimal prevRetention = priorOpt.map(ProjectProgressClaim::getCumulativeRetentionAmount).orElse(BigDecimal.ZERO);
        BigDecimal cumRetention = prevRetention.add(currRetention);

        BigDecimal prevAdvance = priorOpt.map(ProjectProgressClaim::getCumulativeAdvanceRecoveryAmount).orElse(BigDecimal.ZERO);
        BigDecimal cumAdvance = prevAdvance.add(currAdvance);

        BigDecimal netPayable = currGross
                .subtract(currRetention)
                .subtract(currAdvance)
                .add(taxAmount)
                .subtract(deductionsAmount)
                .setScale(2, RoundingMode.HALF_UP);

        claim.updateTotals(
                prevGross, currGross, cumGross,
                prevRetention, currRetention, cumRetention,
                prevAdvance, currAdvance, cumAdvance,
                taxAmount, deductionsAmount, netPayable
        );
        claimRepository.save(claim);
    }

    private String generateClaimNumber(ClaimType type) {
        int year = LocalDate.now().getYear();
        String prefix = type == ClaimType.OWNER_IPC ? "IPC-OWN" : "IPC-SUB";
        long seq = claimRepository.countByNumberPrefix(prefix + "-" + year) + 1;
        return String.format("%s-%d-%03d", prefix, year, seq);
    }

    private ProjectProgressClaimResponse mapClaimSummaryResponse(ProjectProgressClaim c) {
        String partyName = null;
        if (c.getPartyId() != null) {
            partyName = partyRepository.findById(c.getPartyId())
                    .map(BusinessParty::getName).orElse(null);
        }
        int linesCount = lineRepository.findByClaimIdOrderBySortOrderAsc(c.getId()).size();

        return new ProjectProgressClaimResponse(
                c.getId(),
                c.getClaimNumber(),
                c.getClaimType(),
                c.getClaimKind(),
                c.getClaimSequenceNumber(),
                c.getProjectId(),
                c.getPartyId(),
                partyName,
                c.getPeriodStartDate(),
                c.getPeriodEndDate(),
                c.getSubmissionDate(),
                c.getCurrencyCode(),
                c.getPreviousGrossAmount(),
                c.getCurrentGrossAmount(),
                c.getCumulativeGrossAmount(),
                c.getPreviousRetentionAmount(),
                c.getCurrentRetentionAmount(),
                c.getCumulativeRetentionAmount(),
                c.getPreviousAdvanceRecoveryAmount(),
                c.getCurrentAdvanceRecoveryAmount(),
                c.getCumulativeAdvanceRecoveryAmount(),
                c.getCurrentTaxAmount(),
                c.getCurrentDeductionsAmount(),
                c.getCurrentNetPayableAmount(),
                c.getCumulativeNetPaidAmount(),
                c.getStatus(),
                c.getCertifiedByUserId(),
                c.getCertifiedAt(),
                c.getCertificationNotes(),
                c.getPostedFinanceJournalId(),
                c.getPostedInvoiceId(),
                c.getPostedAt(),
                c.getNotes(),
                linesCount,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getVersion(),
                List.of(),
                List.of()
        );
    }

    private ProgressClaimLineResponse mapLineResponse(ProgressClaimLine l) {
        return new ProgressClaimLineResponse(
                l.getId(),
                l.getClaimId(),
                l.getLineType(),
                l.getWbsNodeId(),
                l.getItemCode(),
                l.getDescription(),
                l.getUnitOfMeasure(),
                l.getContractQuantity(),
                l.getUnitRate(),
                l.getPreviousQuantity(),
                l.getCurrentQuantity(),
                l.getCumulativeQuantity(),
                l.getPreviousAmount(),
                l.getCurrentAmount(),
                l.getCumulativeAmount(),
                l.getPercentComplete(),
                l.getRemarks(),
                l.getSortOrder()
        );
    }

    private ProgressClaimAdjustmentResponse mapAdjustmentResponse(ProgressClaimAdjustment a) {
        return new ProgressClaimAdjustmentResponse(
                a.getId(),
                a.getClaimId(),
                a.getAdjustmentType(),
                a.getDescription(),
                a.getPercentageRate(),
                a.getCalculationBasisAmount(),
                a.getAdjustmentAmount(),
                a.isAddition(),
                a.getNotes()
        );
    }

    private ProjectProgressClaim requireClaim(String claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND"));
    }
}
