package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.close.PeriodCloseOrchestratorService;
import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/period-close")
public class PeriodCloseController {

    private final PeriodCloseOrchestratorService orchestratorService;

    public PeriodCloseController(PeriodCloseOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @GetMapping("/readiness/{periodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public PeriodCloseOrchestratorService.PeriodReadinessReport checkReadiness(@PathVariable String periodId) {
        return orchestratorService.checkReadiness(periodId);
    }

    @PostMapping("/execute/{periodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public List<PeriodCloseExecutionRecord> executeClose(@PathVariable String periodId) {
        return orchestratorService.executeClose(periodId);
    }
}
