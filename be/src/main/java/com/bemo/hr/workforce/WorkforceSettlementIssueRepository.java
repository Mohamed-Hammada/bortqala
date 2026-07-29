package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkforceSettlementIssueRepository extends JpaRepository<WorkforceSettlementIssue, String> {
    List<WorkforceSettlementIssue> findByPeriodIdAndCalculationVersionOrderBySeverityDescWorkerNameAsc(
            String periodId, int calculationVersion);
}
