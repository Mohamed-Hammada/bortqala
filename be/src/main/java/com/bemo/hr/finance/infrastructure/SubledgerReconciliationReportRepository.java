package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubledgerReconciliationReportRepository extends JpaRepository<SubledgerReconciliationReport, String> {
    List<SubledgerReconciliationReport> findByPeriodId(String periodId);
}
