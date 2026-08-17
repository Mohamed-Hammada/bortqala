package com.bemo.hr.trade.sales.application;

import com.bemo.hr.finance.application.close.CloseBlockerQueryService;
import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SalesCloseProvider implements ModuleCloseProvider {
    private final CloseBlockerQueryService queries;

    public SalesCloseProvider(CloseBlockerQueryService queries) {
        this.queries = queries;
    }

    private long blockers(String periodId) {
        return queries.dated(periodId, "sales_orders", "so_date", "status in ('DRAFT','CONFIRMED')")
                + queries.dated(periodId, "customer_invoices", "invoice_date", "status = 'DRAFT'");
    }

    @Override
    public String getModuleName() {
        return "SALES";
    }

    @Override
    public boolean isPeriodCloseReady(String periodId) {
        return blockers(periodId) == 0;
    }

    @Override
    public Optional<String> getBlockerReason(String periodId) {
        long count = blockers(periodId);
        return count == 0 ? Optional.empty() : Optional.of(count + " sales document(s) remain unfinished");
    }

    @Override
    public void executeClose(String periodId) {
        // Sales/O2C period close execution
    }
}
