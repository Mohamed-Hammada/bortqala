package com.bemo.hr.workforce.api;

import com.bemo.hr.workforce.application.WorkforceFinancialIntegrationService;
import com.bemo.hr.workforce.domain.WorkforceGlPosting;
import com.bemo.hr.workforce.domain.WorkforceInvoiceMatch;
import com.bemo.hr.workforce.domain.WorkforceRequestBudget;
import com.bemo.hr.workforce.domain.WorkforceTreasuryMatch;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/workforce/finance")
public class WorkforceFinancialIntegrationController {

    private final WorkforceFinancialIntegrationService financialService;

    public WorkforceFinancialIntegrationController(WorkforceFinancialIntegrationService financialService) {
        this.financialService = financialService;
    }

    public record AllocateBudgetPayload(String requestId, String departmentId, String budgetId, BigDecimal amount) {}
    public record MatchInvoicePayload(String settlementId, String invoiceId, BigDecimal matchedAmount, BigDecimal varianceAmount) {}
    public record RecordGlPostingPayload(String settlementId, String journalId, BigDecimal postedAmount) {}
    public record MatchTreasuryPayload(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {}

    @PostMapping("/budget")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER')")
    public WorkforceRequestBudget allocateRequestBudget(@RequestBody AllocateBudgetPayload payload) {
        return financialService.allocateRequestBudget(payload.requestId(), payload.departmentId(), payload.budgetId(), payload.amount());
    }

    @PostMapping("/invoice-matches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER')")
    public WorkforceInvoiceMatch matchInvoice(@RequestBody MatchInvoicePayload payload) {
        return financialService.matchInvoice(payload.settlementId(), payload.invoiceId(), payload.matchedAmount(), payload.varianceAmount());
    }

    @PostMapping("/gl-postings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public WorkforceGlPosting recordGlPosting(@RequestBody RecordGlPostingPayload payload) {
        return financialService.recordGlPosting(payload.settlementId(), payload.journalId(), payload.postedAmount());
    }

    @PostMapping("/treasury-matches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'TREASURY_MANAGER')")
    public WorkforceTreasuryMatch matchTreasuryPayment(@RequestBody MatchTreasuryPayload payload) {
        return financialService.matchTreasuryPayment(payload.paymentId(), payload.bankTransactionId(), payload.matchedAmount());
    }

    @GetMapping("/budget/{requestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public WorkforceRequestBudget getRequestBudget(@PathVariable String requestId) {
        return financialService.getRequestBudget(requestId);
    }
}
