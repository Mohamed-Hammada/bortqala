package com.bemo.hr.finance.application;

import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.SubledgerReconciliationReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class SubledgerReconciliationService {

    private final SubledgerReconciliationReportRepository repository;
    private final List<SubledgerReconciliationProvider> providers;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final ObjectMapper objectMapper;

    public SubledgerReconciliationService(SubledgerReconciliationReportRepository repository,
                                          List<SubledgerReconciliationProvider> providers) {
        this(repository, providers, null, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SubledgerReconciliationService(SubledgerReconciliationReportRepository repository,
                                          List<SubledgerReconciliationProvider> providers,
                                          FiscalPeriodRepository fiscalPeriodRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.providers = providers;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubledgerReconciliationReport generateReport(String periodId, SubledgerReconciliationReport.SubledgerType subledgerType, BigDecimal glBalance, BigDecimal subledgerBalance) {
        BigDecimal finalGl = glBalance;
        BigDecimal finalSub = subledgerBalance;
        LocalDate asOf = fiscalPeriodRepository == null ? LocalDate.now() : fiscalPeriodRepository.findById(periodId).orElseThrow().getEndDate();
        String details = "[]";

        for (SubledgerReconciliationProvider provider : providers) {
            if (provider.type() == subledgerType) {
                var calc = provider.calculate(periodId, asOf);
                if (finalGl == null) finalGl = calc.glBalance();
                if (finalSub == null) finalSub = calc.subledgerBalance();
                try { details = objectMapper.writeValueAsString(calc.sourceDifferences()); }
                catch (Exception ex) { throw new IllegalStateException("Cannot serialize reconciliation differences", ex); }
                break;
            }
        }

        if (finalGl == null) finalGl = BigDecimal.ZERO;
        if (finalSub == null) finalSub = BigDecimal.ZERO;

        SubledgerReconciliationReport report = new SubledgerReconciliationReport(periodId, subledgerType, finalGl, finalSub, asOf, details);
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public List<SubledgerReconciliationReport> getReportsByPeriod(String periodId) {
        return repository.findByPeriodId(periodId);
    }
}
