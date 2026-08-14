package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.SubledgerReconciliationService;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/reconciliation/subledger")
public class SubledgerReconciliationController {

    private final SubledgerReconciliationService reconciliationService;

    public SubledgerReconciliationController(SubledgerReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    public record GenerateReportPayload(String periodId, String subledgerType) {}

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT')")
    public SubledgerReconciliationReport generateReport(@RequestBody GenerateReportPayload payload) {
        return reconciliationService.generateReport(payload.periodId(),
                SubledgerReconciliationReport.SubledgerType.valueOf(payload.subledgerType()));
    }

    @GetMapping("/periods/{periodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR')")
    public List<SubledgerReconciliationReport> getReportsByPeriod(@PathVariable String periodId) {
        return reconciliationService.getReportsByPeriod(periodId);
    }
}
