package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.application.CloseChecklistService;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.finance.infrastructure.PeriodCloseExecutionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PeriodCloseOrchestratorService {

    private final List<ModuleCloseProvider> closeProviders;
    private final PeriodCloseExecutionRepository repository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final CloseChecklistService closeChecklistService;

    public PeriodCloseOrchestratorService(List<ModuleCloseProvider> closeProviders,
                                          PeriodCloseExecutionRepository repository,
                                          FiscalPeriodRepository fiscalPeriodRepository,
                                          CloseChecklistService closeChecklistService) {
        this.closeProviders = closeProviders;
        this.repository = repository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.closeChecklistService = closeChecklistService;
    }

    @Transactional(readOnly = true)
    public PeriodReadinessReport checkReadiness(String periodId) {
        log.debug("checkReadiness called with periodId={}", periodId);
        List<ModuleReadinessStatus> statuses = new ArrayList<>();
        boolean allReady = true;

        for (ModuleCloseProvider provider : closeProviders) {
            boolean ready;
            String blockerReason;
            try {
                ready = provider.isPeriodCloseReady(periodId);
                blockerReason = ready ? null : provider.getBlockerReason(periodId).orElse("Unresolved period dependencies");
            } catch (RuntimeException ex) {
                ready = false;
                blockerReason = "Readiness check failed: " + ex.getClass().getSimpleName();
                log.warn("Readiness check failed for module {}: {}", provider.getModuleName(), ex.getMessage());
            }
            if (!ready) {
                allReady = false;
            }
            statuses.add(new ModuleReadinessStatus(provider.getModuleName(), ready, blockerReason));
        }

        log.info("checkReadiness completed for period {}, allReady={}", periodId, allReady);
        return new PeriodReadinessReport(periodId, allReady, statuses);
    }

    @Transactional
    public List<PeriodCloseExecutionRecord> executeClose(String periodId, String actor, Long expectedVersion) {
        log.debug("executeClose called with periodId={}, actor={}, expectedVersion={}", periodId, actor, expectedVersion);
        FiscalPeriod period = fiscalPeriodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new BusinessRuleException("Fiscal period not found",
                        "FIN_FISCAL_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (expectedVersion != null && expectedVersion != period.getVersion()) {
            log.warn("Version conflict for period {}: expected={}, actual={}", periodId, expectedVersion, period.getVersion());
            throw new BusinessRuleException("The fiscal period was changed by another request.",
                    "RECORD_ALREADY_MODIFIED", HttpStatus.CONFLICT);
        }
        if (period.getStatus() == FiscalPeriod.Status.CLOSED || period.getStatus() == FiscalPeriod.Status.LOCKED) {
            log.debug("Period {} already closed/locked, returning existing records", periodId);
            return repository.findByPeriodId(periodId);
        }
        if (!closeChecklistService.computePrecheck(periodId).canClose()) {
            log.warn("Period close blocked by precheck for period {}", periodId);
            throw new BusinessRuleException("Period close blocked by financial reconciliation checks",
                    "FISCAL_PERIOD_PRECHECK_FAILED", HttpStatus.CONFLICT);
        }
        PeriodReadinessReport readiness = checkReadiness(periodId);
        if (!readiness.allReady()) {
            log.warn("Period close blocked by module readiness for period {}", periodId);
            throw new BusinessRuleException("Period close blocked by module readiness checks", "PERIOD_CLOSE_BLOCKED", HttpStatus.CONFLICT);
        }

        List<PeriodCloseExecutionRecord> results = new ArrayList<>();
        for (ModuleCloseProvider provider : closeProviders) {
            provider.executeClose(periodId);
            PeriodCloseExecutionRecord record = new PeriodCloseExecutionRecord(periodId, provider.getModuleName(), PeriodCloseExecutionRecord.Status.CLOSED, null);
            results.add(repository.save(record));
        }

        period.updateStatus(FiscalPeriod.Status.CLOSED, actor);
        fiscalPeriodRepository.save(period);

        log.info("FiscalPeriod {} closed successfully by actor={}", periodId, actor);
        return results;
    }

    public record ModuleReadinessStatus(String moduleName, boolean ready, String blockerReason) {
    }

    public record PeriodReadinessReport(String periodId, boolean allReady, List<ModuleReadinessStatus> modules) {
    }
}
