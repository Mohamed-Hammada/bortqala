package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.SubledgerReconciliationService;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.shared.security.Roles;
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

    @PostMapping("/generate")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.FINANCE_MANAGER)
    public SubledgerReconciliationReport generateReport(@RequestBody GenerateReportPayload payload) {
        return reconciliationService.generateReport(payload.periodId(),
                SubledgerReconciliationReport.SubledgerType.valueOf(payload.subledgerType()));
    }

    @GetMapping("/periods/{periodId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.AUDITOR + " or " + Roles.FINANCE_MANAGER)
    public List<SubledgerReconciliationReport> getReportsByPeriod(@PathVariable String periodId) {
        return reconciliationService.getReportsByPeriod(periodId);
    }

    public record GenerateReportPayload(String periodId, String subledgerType) {
    }
}
