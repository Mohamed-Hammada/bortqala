package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.DeterministicPolicyEngineService;
import com.bemo.hr.finance.domain.rules.AccountingRulePolicy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/rules/policies")
public class DeterministicPolicyController {

    private final DeterministicPolicyEngineService policyService;

    public DeterministicPolicyController(DeterministicPolicyEngineService policyService) {
        this.policyService = policyService;
    }

    public record CreatePolicyPayload(String policyCode, String description, String triggerEvent, String debitAccountPattern, String creditAccountPattern) {}
    public record EvaluatePolicyPayload(String policyCode, BigDecimal amount) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public AccountingRulePolicy createPolicy(@RequestBody CreatePolicyPayload payload) {
        return policyService.createPolicy(payload.policyCode(), payload.description(), payload.triggerEvent(), payload.debitAccountPattern(), payload.creditAccountPattern());
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public DeterministicPolicyEngineService.PolicyEvaluationResult evaluatePolicy(@RequestBody EvaluatePolicyPayload payload) {
        return policyService.evaluatePolicy(payload.policyCode(), payload.amount());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public List<AccountingRulePolicy> getAllPolicies() {
        return policyService.getAllPolicies();
    }
}
