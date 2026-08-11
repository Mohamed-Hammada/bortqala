package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.SubledgerReconciliationReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SubledgerReconciliationService {

    private final SubledgerReconciliationReportRepository repository;

    public SubledgerReconciliationService(SubledgerReconciliationReportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SubledgerReconciliationReport generateReport(String periodId, SubledgerReconciliationReport.SubledgerType subledgerType, BigDecimal glBalance, BigDecimal subledgerBalance) {
        SubledgerReconciliationReport report = new SubledgerReconciliationReport(periodId, subledgerType, glBalance, subledgerBalance);
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public List<SubledgerReconciliationReport> getReportsByPeriod(String periodId) {
        return repository.findByPeriodId(periodId);
    }
}
