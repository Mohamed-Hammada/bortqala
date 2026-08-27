package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, String> {
    List<CommissionRule> findByActiveTrue();
    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
