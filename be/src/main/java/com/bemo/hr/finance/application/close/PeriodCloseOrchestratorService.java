package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import com.bemo.hr.finance.infrastructure.PeriodCloseExecutionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PeriodCloseOrchestratorService {

    private final List<ModuleCloseProvider> closeProviders;
    private final PeriodCloseExecutionRepository repository;

    public PeriodCloseOrchestratorService(List<ModuleCloseProvider> closeProviders, PeriodCloseExecutionRepository repository) {
        this.closeProviders = closeProviders;
        this.repository = repository;
    }

    public record ModuleReadinessStatus(String moduleName, boolean ready, String blockerReason) {}
    public record PeriodReadinessReport(String periodId, boolean allReady, List<ModuleReadinessStatus> modules) {}

    @Transactional(readOnly = true)
    public PeriodReadinessReport checkReadiness(String periodId) {
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
            }
            if (!ready) {
                allReady = false;
            }
            statuses.add(new ModuleReadinessStatus(provider.getModuleName(), ready, blockerReason));
        }

        return new PeriodReadinessReport(periodId, allReady, statuses);
    }

    @Transactional
    public List<PeriodCloseExecutionRecord> executeClose(String periodId) {
        PeriodReadinessReport readiness = checkReadiness(periodId);
        if (!readiness.allReady()) {
            throw new BusinessRuleException("Period close blocked by module readiness checks", "PERIOD_CLOSE_BLOCKED", HttpStatus.CONFLICT);
        }

        List<PeriodCloseExecutionRecord> results = new ArrayList<>();
        for (ModuleCloseProvider provider : closeProviders) {
            provider.executeClose(periodId);
            PeriodCloseExecutionRecord record = new PeriodCloseExecutionRecord(periodId, provider.getModuleName(), PeriodCloseExecutionRecord.Status.CLOSED, null);
            results.add(repository.save(record));
        }

        return results;
    }
}
