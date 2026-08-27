package com.bemo.hr.automation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DunningRuleRepository extends JpaRepository<DunningRule, String> {
    List<DunningRule> findByAppIdAndActiveTrueOrderByDaysOverdueAsc(String appId);
    List<DunningRule> findByAppIdOrderByDaysOverdueAsc(String appId);
}
