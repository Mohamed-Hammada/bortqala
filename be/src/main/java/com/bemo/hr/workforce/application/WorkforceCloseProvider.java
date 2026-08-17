package com.bemo.hr.workforce.application;

import com.bemo.hr.finance.application.close.CloseBlockerQueryService;
import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkforceCloseProvider implements ModuleCloseProvider {
    private final CloseBlockerQueryService queries;

    public WorkforceCloseProvider(CloseBlockerQueryService queries) {
        this.queries = queries;
    }

    private long blockers(String periodId) {
        return queries.dated(periodId, "workforce_settlement_periods", "CAST(start_date AS DATE)", "status not in ('POSTED','PAID')");
    }

    @Override
    public String getModuleName() {
        return "WORKFORCE";
    }

    @Override
    public boolean isPeriodCloseReady(String periodId) {
        return blockers(periodId) == 0;
    }

    @Override
    public Optional<String> getBlockerReason(String periodId) {
        long count = blockers(periodId);
        return count == 0 ? Optional.empty() : Optional.of(count + " workforce settlement period(s) remain unfinished");
    }

    @Override
    public void executeClose(String periodId) {
        // Workforce period close execution
    }
}
