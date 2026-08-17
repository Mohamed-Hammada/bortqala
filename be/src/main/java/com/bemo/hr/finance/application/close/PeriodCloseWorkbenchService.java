package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import com.bemo.hr.finance.infrastructure.PeriodCloseExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PeriodCloseWorkbenchService {

    private final List<ModuleCloseProvider> providers;
    private final PeriodCloseExecutionRepository executionRepository;

    public PeriodCloseWorkbenchService(List<ModuleCloseProvider> providers, PeriodCloseExecutionRepository executionRepository) {
        this.providers = providers != null ? providers : List.of();
        this.executionRepository = executionRepository;
    }

    @Transactional(readOnly = true)
    public WorkbenchSummary getWorkbenchSummary(String periodId) {
        Map<String, PeriodCloseExecutionRecord> executedMap = executionRepository.findByPeriodId(periodId).stream()
                .filter(rec -> rec.getStatus() == PeriodCloseExecutionRecord.Status.CLOSED)
                .collect(Collectors.toMap(PeriodCloseExecutionRecord::getModuleName, rec -> rec, (a, b) -> a));

        List<ModuleStatus> statuses = new ArrayList<>();
        int readyCount = 0;
        int executedCount = 0;

        for (ModuleCloseProvider provider : providers) {
            String name = provider.getModuleName();
            boolean isReady;
            String blocker;
            try {
                isReady = provider.isPeriodCloseReady(periodId);
                blocker = isReady ? null : provider.getBlockerReason(periodId).orElse("Unresolved period dependencies");
            } catch (RuntimeException ex) {
                isReady = false;
                blocker = "Readiness check failed: " + ex.getClass().getSimpleName();
            }
            boolean isExecuted = executedMap.containsKey(name);

            if (isReady) readyCount++;
            if (isExecuted) executedCount++;

            statuses.add(new ModuleStatus(name, isReady, blocker, isExecuted));
        }

        List<PeriodCloseExecutionRecord> recentExecutions = executionRepository.findByPeriodId(periodId);
        return new WorkbenchSummary(periodId, providers.size(), readyCount, executedCount, statuses, recentExecutions);
    }

    public record ModuleStatus(String moduleName, boolean isReady, String blockerReason, boolean isExecuted) {
    }

    public record WorkbenchSummary(String periodId, int totalModules, int readyModules, int executedModules,
                                   List<ModuleStatus> moduleStatuses,
                                   List<PeriodCloseExecutionRecord> recentExecutions) {
    }
}
