package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.JournalApprovalRule;
import com.bemo.hr.finance.infrastructure.JournalApprovalRuleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class JournalApprovalService {

    private final JournalApprovalRuleRepository repository;

    public JournalApprovalService(JournalApprovalRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public JournalApprovalRule setApprovalRule(String accountId, BigDecimal maxAmountWithoutApproval, boolean requiresApproval) {
        JournalApprovalRule rule = repository.findByAccountId(accountId)
                .orElseGet(() -> new JournalApprovalRule(accountId, maxAmountWithoutApproval, requiresApproval));
        rule.update(maxAmountWithoutApproval, requiresApproval);
        return repository.save(rule);
    }

    @Transactional(readOnly = true)
    public boolean isApprovalRequired(String accountId, BigDecimal amount) {
        return repository.findByAccountId(accountId)
                .map(rule -> rule.isRequiresApproval() && amount.compareTo(rule.getMaxAmountWithoutApproval()) > 0)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public JournalApprovalRule getApprovalRule(String accountId) {
        return repository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessRuleException("Approval rule not found for account", "APPROVAL_RULE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
