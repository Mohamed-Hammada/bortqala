package com.bemo.hr.finance.application;

import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.SubledgerReconciliationReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SubledgerReconciliationService {

    private final SubledgerReconciliationReportRepository repository;
    private final List<SubledgerReconciliationProvider> providers;

    public SubledgerReconciliationService(SubledgerReconciliationReportRepository repository,
                                          List<SubledgerReconciliationProvider> providers) {
        this.repository = repository;
        this.providers = providers;
    }

    @Transactional
    public SubledgerReconciliationReport generateReport(String periodId, SubledgerReconciliationReport.SubledgerType subledgerType, BigDecimal glBalance, BigDecimal subledgerBalance) {
        BigDecimal finalGl = glBalance;
        BigDecimal finalSub = subledgerBalance;

        for (SubledgerReconciliationProvider provider : providers) {
            if (provider.type() == subledgerType) {
                var calc = provider.calculate(periodId, LocalDate.now());
                if (finalGl == null) finalGl = calc.glBalance();
                if (finalSub == null) finalSub = calc.subledgerBalance();
                break;
            }
        }

        if (finalGl == null) finalGl = BigDecimal.ZERO;
        if (finalSub == null) finalSub = BigDecimal.ZERO;

        SubledgerReconciliationReport report = new SubledgerReconciliationReport(periodId, subledgerType, finalGl, finalSub);
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public List<SubledgerReconciliationReport> getReportsByPeriod(String periodId) {
        return repository.findByPeriodId(periodId);
    }
}
