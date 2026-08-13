package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.close.PeriodCloseWorkbenchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/close/workbench")
public class PeriodCloseWorkbenchController {

    private final PeriodCloseWorkbenchService workbenchService;

    public PeriodCloseWorkbenchController(PeriodCloseWorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/{periodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public PeriodCloseWorkbenchService.WorkbenchSummary getWorkbenchSummary(@PathVariable String periodId) {
        return workbenchService.getWorkbenchSummary(periodId);
    }
}
