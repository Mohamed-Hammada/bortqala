package com.bemo.hr.operations.application;

import com.bemo.hr.finance.application.close.CloseBlockerQueryService;
import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InventoryCloseProvider implements ModuleCloseProvider {
    private final CloseBlockerQueryService queries;

    public InventoryCloseProvider(CloseBlockerQueryService queries) {
        this.queries = queries;
    }

    public String getModuleName() {
        return "INVENTORY";
    }

    private long blockers(String periodId) {
        return queries.dated(periodId, "stock_transfer_headers", "transfer_date", "status in ('DRAFT','SHIPPED')")
                + queries.dated(periodId, "cycle_count_headers", "count_date", "status not in ('ADJUSTED','CANCELLED')");
    }

    public boolean isPeriodCloseReady(String periodId) {
        return blockers(periodId) == 0;
    }

    public Optional<String> getBlockerReason(String periodId) {
        long count = blockers(periodId);
        return count == 0 ? Optional.empty() : Optional.of(count + " inventory control document(s) remain unfinished");
    }

    public void executeClose(String periodId) {
    }
}
