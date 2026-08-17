package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.JournalApprovalRule;
import com.bemo.hr.finance.infrastructure.JournalApprovalRuleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class JournalApprovalService {

    private final JournalApprovalRuleRepository journalApprovalRuleRepository;

    public JournalApprovalService(JournalApprovalRuleRepository journalApprovalRuleRepository) {
        this.journalApprovalRuleRepository = journalApprovalRuleRepository;
    }

    @Transactional
    public JournalApprovalRule setApprovalRule(String accountId, BigDecimal maxAmountWithoutApproval, boolean requiresApproval) {
        JournalApprovalRule rule = journalApprovalRuleRepository.findByAccountId(accountId)
                .orElseGet(() -> new JournalApprovalRule(accountId, maxAmountWithoutApproval, requiresApproval));
        rule.update(maxAmountWithoutApproval, requiresApproval);
        return journalApprovalRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public boolean isApprovalRequired(String accountId, BigDecimal amount) {
        return journalApprovalRuleRepository.findByAccountId(accountId)
                .map(rule -> rule.isRequiresApproval() && amount.compareTo(rule.getMaxAmountWithoutApproval()) > 0)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean isApprovalRequired(Map<String, BigDecimal> amountsByAccount) {
        return amountsByAccount.entrySet().stream()
                .anyMatch(entry -> isApprovalRequired(entry.getKey(), entry.getValue()));
    }

    @Transactional(readOnly = true)
    public JournalApprovalRule getApprovalRule(String accountId) {
        return journalApprovalRuleRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessRuleException("Approval rule not found for account", "APPROVAL_RULE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
