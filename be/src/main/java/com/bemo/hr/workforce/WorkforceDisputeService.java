package com.bemo.hr.workforce;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WorkforceDisputeService {

    private final WorkforceDisputeRepository disputeRepository;

    public WorkforceDisputeService(WorkforceDisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
    }

    @Transactional
    public WorkforceDispute createDispute(String settlementPeriodId, String contractorId, BigDecimal amount, String reason) {
        WorkforceDispute dispute = new WorkforceDispute(settlementPeriodId, contractorId, amount, reason);
        return disputeRepository.save(dispute);
    }

    @Transactional
    public WorkforceDispute submitForReview(String disputeId) {
        WorkforceDispute dispute = getDispute(disputeId);
        dispute.submitForReview();
        return disputeRepository.save(dispute);
    }

    @Transactional
    public WorkforceDispute resolveDispute(String disputeId, String resolutionNotes, String username) {
        WorkforceDispute dispute = getDispute(disputeId);
        dispute.resolve(resolutionNotes, username);
        return disputeRepository.save(dispute);
    }

    @Transactional
    public WorkforceDispute rejectDispute(String disputeId, String reason, String username) {
        WorkforceDispute dispute = getDispute(disputeId);
        dispute.reject(reason, username);
        return disputeRepository.save(dispute);
    }

    @Transactional(readOnly = true)
    public List<WorkforceDispute> getDisputesByPeriod(String settlementPeriodId) {
        return disputeRepository.findBySettlementPeriodId(settlementPeriodId);
    }

    private WorkforceDispute getDispute(String disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessRuleException("Workforce dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
