package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.JournalApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalApprovalRuleRepository extends JpaRepository<JournalApprovalRule, String> {
    Optional<JournalApprovalRule> findByAccountId(String accountId);
}
