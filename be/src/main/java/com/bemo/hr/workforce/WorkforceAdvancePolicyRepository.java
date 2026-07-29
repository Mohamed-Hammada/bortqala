package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkforceAdvancePolicyRepository extends JpaRepository<WorkforceAdvancePolicy, String> {
    List<WorkforceAdvancePolicy> findAllByOrderByScopeTypeAscScopeIdAsc();
}
