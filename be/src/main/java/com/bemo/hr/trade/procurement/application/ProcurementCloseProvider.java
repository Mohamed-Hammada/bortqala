package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.finance.application.close.CloseBlockerQueryService;
import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ProcurementCloseProvider implements ModuleCloseProvider {
    private final CloseBlockerQueryService queries;
    public ProcurementCloseProvider(CloseBlockerQueryService queries) { this.queries = queries; }
    public String getModuleName() { return "PROCUREMENT"; }
    private long blockers(String periodId) { return queries.dated(periodId, "purchase_orders", "po_date", "status in ('DRAFT','ISSUED','PARTIALLY_RECEIVED')"); }
    public boolean isPeriodCloseReady(String periodId) { return blockers(periodId) == 0; }
    public Optional<String> getBlockerReason(String periodId) { long count = blockers(periodId); return count == 0 ? Optional.empty() : Optional.of(count + " purchase order(s) remain unfinished"); }
    public void executeClose(String periodId) { }
}
