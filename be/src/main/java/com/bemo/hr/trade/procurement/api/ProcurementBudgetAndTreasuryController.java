package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.ProcurementBudgetAndTreasuryService;
import com.bemo.hr.trade.procurement.domain.ProcurementBudgetApproval;
import com.bemo.hr.trade.procurement.domain.ProcurementTreasuryBankMatch;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/procurement/governance")
public class ProcurementBudgetAndTreasuryController {

    private final ProcurementBudgetAndTreasuryService governanceService;

    public ProcurementBudgetAndTreasuryController(ProcurementBudgetAndTreasuryService governanceService) {
        this.governanceService = governanceService;
    }

    @PostMapping("/budget-approvals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    public ProcurementBudgetApproval approveBudget(@RequestBody ApproveBudgetPayload payload) {
        return governanceService.approveBudget(payload.requisitionId(), payload.budgetId(), payload.amount());
    }

    @PostMapping("/treasury-matches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'TREASURY_USER')")
    public ProcurementTreasuryBankMatch matchTreasuryPayment(@RequestBody MatchTreasuryPayload payload) {
        return governanceService.matchTreasuryPayment(payload.paymentId(), payload.bankTransactionId(), payload.matchedAmount());
    }

    @GetMapping("/budget-approvals/{requisitionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public ProcurementBudgetApproval getBudgetApproval(@PathVariable String requisitionId) {
        return governanceService.getBudgetApproval(requisitionId);
    }

    public record ApproveBudgetPayload(String requisitionId, String budgetId, BigDecimal amount) {
    }

    public record MatchTreasuryPayload(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {
    }
}
