package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.close.PeriodCloseWorkbenchService;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/close/workbench")
public class PeriodCloseWorkbenchController {

    private final PeriodCloseWorkbenchService workbenchService;

    public PeriodCloseWorkbenchController(PeriodCloseWorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/{periodId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.VIEWER)
    public PeriodCloseWorkbenchService.WorkbenchSummary getWorkbenchSummary(@PathVariable String periodId) {
        return workbenchService.getWorkbenchSummary(periodId);
    }
}
