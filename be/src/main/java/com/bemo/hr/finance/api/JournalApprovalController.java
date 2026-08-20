package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.JournalApprovalService;
import com.bemo.hr.finance.domain.JournalApprovalRule;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/finance/journal-approval-rules")
public class JournalApprovalController {

    private final JournalApprovalService approvalService;

    public JournalApprovalController(JournalApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public JournalApprovalRule setApprovalRule(@RequestBody SetApprovalRulePayload payload) {
        return approvalService.setApprovalRule(payload.accountId(), payload.maxAmountWithoutApproval(), payload.requiresApproval());
    }

    @GetMapping("/accounts/{accountId}")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_VIEWER)
    public JournalApprovalRule getApprovalRule(@PathVariable String accountId) {
        return approvalService.getApprovalRule(accountId);
    }

    public record SetApprovalRulePayload(String accountId, BigDecimal maxAmountWithoutApproval,
                                         boolean requiresApproval) {
    }
}
