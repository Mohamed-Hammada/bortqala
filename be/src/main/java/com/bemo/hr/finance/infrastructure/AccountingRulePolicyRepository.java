package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.rules.AccountingRulePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingRulePolicyRepository extends JpaRepository<AccountingRulePolicy, String> {
    Optional<AccountingRulePolicy> findByPolicyCode(String policyCode);
}
