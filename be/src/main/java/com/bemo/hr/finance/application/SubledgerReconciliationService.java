package com.bemo.hr.finance.application;

import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.finance.infrastructure.SubledgerReconciliationReportRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

@Slf4j
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
    public SubledgerReconciliationReport generateReport(String periodId,
                                                        SubledgerReconciliationReport.SubledgerType subledgerType) {
        log.debug("generateReport called with periodId={}, subledgerType={}", periodId, subledgerType);
        if (fiscalPeriodRepository == null) {
            throw unavailable(subledgerType);
        }
        LocalDate asOf = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("Fiscal period not found.",
                        "FIN_FISCAL_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND))
                .getEndDate();
        SubledgerReconciliationProvider provider = providers.stream()
                .filter(candidate -> candidate.type() == subledgerType)
                .findFirst().orElseThrow(() -> unavailable(subledgerType));
        var calculation = provider.calculate(periodId, asOf);
        if (calculation == null || calculation.glBalance() == null || calculation.subledgerBalance() == null) {
            throw unavailable(subledgerType);
        }
        String details;
        try {
            details = objectMapper.writeValueAsString(calculation.sourceDifferences());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize reconciliation differences", ex);
        }

        SubledgerReconciliationReport report = new SubledgerReconciliationReport(periodId, subledgerType,
                calculation.glBalance(), calculation.subledgerBalance(), asOf, details);
        SubledgerReconciliationReport saved = repository.save(report);
        log.info("SubledgerReconciliationReport {} generated for period {} type {}", saved.getId(), periodId, subledgerType);
        return saved;
    }

    private BusinessRuleException unavailable(SubledgerReconciliationReport.SubledgerType type) {
        return new BusinessRuleException("No authoritative reconciliation provider is configured for " + type + ".",
                "FIN_RECONCILIATION_PROVIDER_REQUIRED", HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Transactional(readOnly = true)
    public List<SubledgerReconciliationReport> getReportsByPeriod(String periodId) {
        return repository.findByPeriodId(periodId);
    }
}
