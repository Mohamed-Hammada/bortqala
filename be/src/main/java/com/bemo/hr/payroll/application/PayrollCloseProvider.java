package com.bemo.hr.payroll.application;

import com.bemo.hr.finance.application.close.CloseBlockerQueryService;
import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PayrollCloseProvider implements ModuleCloseProvider {
    private final CloseBlockerQueryService queries;

    public PayrollCloseProvider(CloseBlockerQueryService queries) {
        this.queries = queries;
    }

    private long blockers(String periodId) {
        return queries.dated(periodId, "pay_periods", "start_date", "status <> 'CLOSED'");
    }

    @Override
    public String getModuleName() {
        return "PAYROLL";
    }

    @Override
    public boolean isPeriodCloseReady(String periodId) {
        return blockers(periodId) == 0;
    }

    @Override
    public Optional<String> getBlockerReason(String periodId) {
        long count = blockers(periodId);
        return count == 0 ? Optional.empty() : Optional.of(count + " payroll period(s) remain open");
    }

    @Override
    public void executeClose(String periodId) {
        // Payroll period close execution
    }
}
