package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.ProcurementBudgetApproval;
import com.bemo.hr.trade.procurement.domain.ProcurementTreasuryBankMatch;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementBudgetApprovalRepository;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementTreasuryBankMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
        ProcurementBudgetApproval approval = budgetApprovalRepository.findByRequisitionId(requisitionId)
                .orElseGet(() -> new ProcurementBudgetApproval(requisitionId, budgetId, amount));
        return budgetApprovalRepository.save(approval);
    }

    @Transactional
    public ProcurementTreasuryBankMatch matchTreasuryPayment(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {
        ProcurementTreasuryBankMatch match = treasuryBankMatchRepository.findByPaymentId(paymentId)
                .orElseGet(() -> new ProcurementTreasuryBankMatch(paymentId, bankTransactionId, matchedAmount));
        return treasuryBankMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public ProcurementBudgetApproval getBudgetApproval(String requisitionId) {
        return budgetApprovalRepository.findByRequisitionId(requisitionId)
                .orElseThrow(() -> new BusinessRuleException("Budget approval not found", "BUDGET_APPROVAL_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
