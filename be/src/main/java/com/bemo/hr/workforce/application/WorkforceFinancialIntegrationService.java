package com.bemo.hr.workforce.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.workforce.domain.WorkforceGlPosting;
import com.bemo.hr.workforce.domain.WorkforceInvoiceMatch;
import com.bemo.hr.workforce.domain.WorkforceRequestBudget;
import com.bemo.hr.workforce.domain.WorkforceTreasuryMatch;
import com.bemo.hr.workforce.infrastructure.WorkforceGlPostingRepository;
import com.bemo.hr.workforce.infrastructure.WorkforceInvoiceMatchRepository;
import com.bemo.hr.workforce.infrastructure.WorkforceRequestBudgetRepository;
import com.bemo.hr.workforce.infrastructure.WorkforceTreasuryMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WorkforceFinancialIntegrationService {

    private final WorkforceRequestBudgetRepository budgetRepository;
    private final WorkforceInvoiceMatchRepository invoiceMatchRepository;
    private final WorkforceGlPostingRepository glPostingRepository;
    private final WorkforceTreasuryMatchRepository treasuryMatchRepository;

    public WorkforceFinancialIntegrationService(WorkforceRequestBudgetRepository budgetRepository,
                                                WorkforceInvoiceMatchRepository invoiceMatchRepository,
                                                WorkforceGlPostingRepository glPostingRepository,
                                                WorkforceTreasuryMatchRepository treasuryMatchRepository) {
        this.budgetRepository = budgetRepository;
        this.invoiceMatchRepository = invoiceMatchRepository;
        this.glPostingRepository = glPostingRepository;
        this.treasuryMatchRepository = treasuryMatchRepository;
    }

    @Transactional
    public WorkforceRequestBudget allocateRequestBudget(String requestId, String departmentId, String budgetId, BigDecimal amount) {
        WorkforceRequestBudget budget = budgetRepository.findByRequestId(requestId)
                .orElseGet(() -> new WorkforceRequestBudget(requestId, departmentId, budgetId, amount));
        return budgetRepository.save(budget);
    }

    @Transactional
    public WorkforceInvoiceMatch matchInvoice(String settlementId, String invoiceId, BigDecimal matchedAmount, BigDecimal varianceAmount) {
        WorkforceInvoiceMatch match = invoiceMatchRepository.findBySettlementId(settlementId)
                .orElseGet(() -> new WorkforceInvoiceMatch(settlementId, invoiceId, matchedAmount, varianceAmount));
        return invoiceMatchRepository.save(match);
    }

    @Transactional
    public WorkforceGlPosting recordGlPosting(String settlementId, String journalId, BigDecimal postedAmount) {
        WorkforceGlPosting posting = glPostingRepository.findBySettlementId(settlementId)
                .orElseGet(() -> new WorkforceGlPosting(settlementId, journalId, postedAmount));
        return glPostingRepository.save(posting);
    }

    @Transactional
    public WorkforceTreasuryMatch matchTreasuryPayment(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {
        WorkforceTreasuryMatch match = treasuryMatchRepository.findByPaymentId(paymentId)
                .orElseGet(() -> new WorkforceTreasuryMatch(paymentId, bankTransactionId, matchedAmount));
        return treasuryMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public WorkforceRequestBudget getRequestBudget(String requestId) {
        return budgetRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessRuleException("Workforce request budget not found", "WORKFORCE_BUDGET_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
