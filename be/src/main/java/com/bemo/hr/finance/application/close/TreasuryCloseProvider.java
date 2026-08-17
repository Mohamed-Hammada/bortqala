package com.bemo.hr.finance.application.close;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TreasuryCloseProvider implements ModuleCloseProvider {
    private final CloseBlockerQueryService queries;

    public TreasuryCloseProvider(CloseBlockerQueryService queries) {
        this.queries = queries;
    }

    public String getModuleName() {
        return "TREASURY";
    }

    private long blockers(String periodId) {
        return queries.timestamped(periodId, "payment_batch_headers", "created_at", "status not in ('DISBURSED','REJECTED')");
    }

    public boolean isPeriodCloseReady(String periodId) {
        return blockers(periodId) == 0;
    }

    public Optional<String> getBlockerReason(String periodId) {
        long count = blockers(periodId);
        return count == 0 ? Optional.empty() : Optional.of(count + " payment batch(es) remain unfinished");
    }

    public void executeClose(String periodId) {
    }
}
