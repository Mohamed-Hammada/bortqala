package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.shared.security.Roles;
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
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.PROCUREMENT_MANAGER)
    public ProcurementBudgetApproval approveBudget(@RequestBody ApproveBudgetPayload payload) {
        return governanceService.approveBudget(payload.requisitionId(), payload.budgetId(), payload.amount());
    }

    @PostMapping("/treasury-matches")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.TREASURY_USER)
    public ProcurementTreasuryBankMatch matchTreasuryPayment(@RequestBody MatchTreasuryPayload payload) {
        return governanceService.matchTreasuryPayment(payload.paymentId(), payload.bankTransactionId(), payload.matchedAmount());
    }

    @GetMapping("/budget-approvals/{requisitionId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.PROCUREMENT_MANAGER + " or " + Roles.VIEWER)
    public ProcurementBudgetApproval getBudgetApproval(@PathVariable String requisitionId) {
        return governanceService.getBudgetApproval(requisitionId);
    }

    public record ApproveBudgetPayload(String requisitionId, String budgetId, BigDecimal amount) {
    }

    public record MatchTreasuryPayload(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {
    }
}
