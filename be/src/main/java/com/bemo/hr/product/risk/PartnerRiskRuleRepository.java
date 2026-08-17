package com.bemo.hr.product.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerRiskRuleRepository extends JpaRepository<PartnerRiskRule, String> {
    Optional<PartnerRiskRule> findFirstBy();
}
