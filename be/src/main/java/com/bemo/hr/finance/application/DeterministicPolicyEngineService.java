package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.rules.AccountingRulePolicy;
import com.bemo.hr.finance.infrastructure.AccountingRulePolicyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class DeterministicPolicyEngineService {

    private final AccountingRulePolicyRepository repository;

    public DeterministicPolicyEngineService(AccountingRulePolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AccountingRulePolicy createPolicy(String policyCode, String description, String triggerEvent, String debitAccountPattern, String creditAccountPattern) {
        log.debug("createPolicy called with policyCode={}, triggerEvent={}", policyCode, triggerEvent);
        AccountingRulePolicy policy = new AccountingRulePolicy(policyCode, description, triggerEvent, debitAccountPattern, creditAccountPattern);
        AccountingRulePolicy saved = repository.save(policy);
        log.info("AccountingRulePolicy {} created successfully", saved.getPolicyCode());
        return saved;
    }

    @Transactional(readOnly = true)
    public PolicyEvaluationResult evaluatePolicy(String policyCode, BigDecimal amount) {
        log.debug("evaluatePolicy called with policyCode={}, amount={}", policyCode, amount);
        AccountingRulePolicy policy = repository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new BusinessRuleException("Accounting rule policy not found", "POLICY_NOT_FOUND", HttpStatus.NOT_FOUND));

        return new PolicyEvaluationResult(policy.getPolicyCode(), policy.getTriggerEvent(), policy.getDebitAccountPattern(), policy.getCreditAccountPattern(), amount);
    }

    @Transactional(readOnly = true)
    public List<AccountingRulePolicy> getAllPolicies() {
        log.debug("getAllPolicies called");
        List<AccountingRulePolicy> result = repository.findAll();
        log.info("getAllPolicies returned {} policies", result.size());
        return result;
    }

    public record PolicyEvaluationResult(
            String policyCode,
            String triggerEvent,
            String debitAccount,
            String creditAccount,
            BigDecimal amount
    ) {
    }
}
