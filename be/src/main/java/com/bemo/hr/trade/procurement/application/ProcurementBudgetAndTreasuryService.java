package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.ProcurementBudgetApproval;
import com.bemo.hr.trade.procurement.domain.ProcurementTreasuryBankMatch;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementBudgetApprovalRepository;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementTreasuryBankMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class ProcurementBudgetAndTreasuryService {

    private final ProcurementBudgetApprovalRepository budgetApprovalRepository;
    private final ProcurementTreasuryBankMatchRepository treasuryBankMatchRepository;

    public ProcurementBudgetAndTreasuryService(ProcurementBudgetApprovalRepository budgetApprovalRepository,
                                               ProcurementTreasuryBankMatchRepository treasuryBankMatchRepository) {
        this.budgetApprovalRepository = budgetApprovalRepository;
        this.treasuryBankMatchRepository = treasuryBankMatchRepository;
    }

    @Transactional
    public ProcurementBudgetApproval approveBudget(String requisitionId, String budgetId, BigDecimal amount) {
        log.debug("approveBudget called with requisitionId={}, budgetId={}, amount={}", requisitionId, budgetId, amount);
        ProcurementBudgetApproval approval = budgetApprovalRepository.findByRequisitionId(requisitionId)
                .orElseGet(() -> new ProcurementBudgetApproval(requisitionId, budgetId, amount));
        ProcurementBudgetApproval saved = budgetApprovalRepository.save(approval);
        log.info("BudgetApproval {} for requisition {} saved successfully", saved.getId(), requisitionId);
        return saved;
    }

    @Transactional
    public ProcurementTreasuryBankMatch matchTreasuryPayment(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {
        log.debug("matchTreasuryPayment called with paymentId={}, bankTransactionId={}, matchedAmount={}", paymentId, bankTransactionId, matchedAmount);
        ProcurementTreasuryBankMatch match = treasuryBankMatchRepository.findByPaymentId(paymentId)
                .orElseGet(() -> new ProcurementTreasuryBankMatch(paymentId, bankTransactionId, matchedAmount));
        ProcurementTreasuryBankMatch saved = treasuryBankMatchRepository.save(match);
        log.info("TreasuryBankMatch {} for payment {} matched successfully", saved.getId(), paymentId);
        return saved;
    }

    @Transactional(readOnly = true)
    public ProcurementBudgetApproval getBudgetApproval(String requisitionId) {
        log.debug("getBudgetApproval called with requisitionId={}", requisitionId);
        ProcurementBudgetApproval approval = budgetApprovalRepository.findByRequisitionId(requisitionId)
                .orElseThrow(() -> {
                    log.warn("Budget approval not found for requisitionId={}", requisitionId);
                    return new BusinessRuleException("Budget approval not found", "BUDGET_APPROVAL_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
        log.debug("getBudgetApproval returned id={} for requisitionId={}", approval.getId(), requisitionId);
        return approval;
    }
}
