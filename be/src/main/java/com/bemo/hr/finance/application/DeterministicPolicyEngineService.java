package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.rules.AccountingRulePolicy;
import com.bemo.hr.finance.infrastructure.AccountingRulePolicyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DeterministicPolicyEngineService {

    private final AccountingRulePolicyRepository repository;

    public DeterministicPolicyEngineService(AccountingRulePolicyRepository repository) {
        this.repository = repository;
    }

    public record PolicyEvaluationResult(
            String policyCode,
            String triggerEvent,
            String debitAccount,
            String creditAccount,
            BigDecimal amount
    ) {}

    @Transactional
    public AccountingRulePolicy createPolicy(String policyCode, String description, String triggerEvent, String debitAccountPattern, String creditAccountPattern) {
        AccountingRulePolicy policy = new AccountingRulePolicy(policyCode, description, triggerEvent, debitAccountPattern, creditAccountPattern);
        return repository.save(policy);
    }

    @Transactional(readOnly = true)
    public PolicyEvaluationResult evaluatePolicy(String policyCode, BigDecimal amount) {
        AccountingRulePolicy policy = repository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new BusinessRuleException("Accounting rule policy not found", "POLICY_NOT_FOUND", HttpStatus.NOT_FOUND));

        return new PolicyEvaluationResult(policy.getPolicyCode(), policy.getTriggerEvent(), policy.getDebitAccountPattern(), policy.getCreditAccountPattern(), amount);
    }

    @Transactional(readOnly = true)
    public List<AccountingRulePolicy> getAllPolicies() {
        return repository.findAll();
    }
}
