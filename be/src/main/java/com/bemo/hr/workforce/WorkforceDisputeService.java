package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class WorkforceDisputeService {

    private final WorkforceDisputeRepository workforceDisputeRepository;
    private final AuditService auditService;

    public WorkforceDisputeService(WorkforceDisputeRepository workforceDisputeRepository, AuditService auditService) {
        this.workforceDisputeRepository = workforceDisputeRepository;
        this.auditService = auditService;
    }

    private static BusinessRuleException rule(String message, String code) {
        return new BusinessRuleException(message, code, HttpStatus.CONFLICT);
    }

    @Transactional
    public WorkforceDispute createDispute(String settlementPeriodId, String contractorId, BigDecimal amount, String reason, String actor) {
        log.debug("createDispute called with settlementPeriodId={}, contractorId={}, amount={}", settlementPeriodId, contractorId, amount);
        if (settlementPeriodId == null || settlementPeriodId.isBlank() || contractorId == null || contractorId.isBlank()) {
            throw rule("Settlement period and contractor are required", "DISPUTE_SCOPE_REQUIRED");
        }
        if (amount == null || amount.signum() <= 0)
            throw rule("Disputed amount must be positive", "DISPUTE_AMOUNT_POSITIVE");
        if (reason == null || reason.isBlank()) throw rule("Dispute reason is required", "DISPUTE_REASON_REQUIRED");
        WorkforceDispute dispute = new WorkforceDispute(settlementPeriodId, contractorId, amount, reason);
        WorkforceDispute saved = workforceDisputeRepository.save(dispute);
        log.info("WorkforceDispute {} created successfully", saved.getId());
        auditService.record("CREATE", "WORKFORCE_DISPUTE", saved.getId(), actor,
                "{\"periodId\":\"" + settlementPeriodId + "\",\"contractorId\":\"" + contractorId + "\"}", null);
        return saved;
    }

    @Transactional
    public WorkforceDispute submitForReview(String disputeId, String actor) {
        log.debug("submitForReview called with disputeId={}", disputeId);
        WorkforceDispute dispute = getDispute(disputeId);
        return transition(dispute, actor, "SUBMIT", () -> dispute.submitForReview());
    }

    @Transactional
    public WorkforceDispute resolveDispute(String disputeId, String resolutionNotes, String username) {
        log.debug("resolveDispute called with disputeId={}", disputeId);
        WorkforceDispute dispute = getDispute(disputeId);
        return transition(dispute, username, "RESOLVE", () -> dispute.resolve(resolutionNotes, username));
    }

    @Transactional
    public WorkforceDispute rejectDispute(String disputeId, String reason, String username) {
        log.debug("rejectDispute called with disputeId={}", disputeId);
        WorkforceDispute dispute = getDispute(disputeId);
        return transition(dispute, username, "REJECT", () -> dispute.reject(reason, username));
    }

    @Transactional(readOnly = true)
    public List<WorkforceDispute> getDisputesByPeriod(String settlementPeriodId) {
        log.debug("getDisputesByPeriod called with settlementPeriodId={}", settlementPeriodId);
        return workforceDisputeRepository.findBySettlementPeriodId(settlementPeriodId);
    }

    private WorkforceDispute getDispute(String disputeId) {
        return workforceDisputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessRuleException("Workforce dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private WorkforceDispute transition(WorkforceDispute dispute, String actor, String action, Runnable transition) {
        WorkforceDispute.Status previous = dispute.getStatus();
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw rule(exception.getMessage(), "DISPUTE_STATUS_INVALID");
        }
        WorkforceDispute saved = workforceDisputeRepository.save(dispute);
        log.info("WorkforceDispute {} {} successfully ({} -> {})", saved.getId(), action, previous, saved.getStatus());
        auditService.record(action, "WORKFORCE_DISPUTE", saved.getId(), actor,
                "{\"from\":\"" + previous + "\",\"to\":\"" + saved.getStatus() + "\"}", null);
        return saved;
    }
}
