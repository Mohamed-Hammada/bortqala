package com.bemo.hr.budget.api;

import com.bemo.hr.budget.application.BudgetVersionService;
import com.bemo.hr.budget.domain.BudgetVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/budget/versions")
public class BudgetVersionController {

    private final BudgetVersionService versionService;

    public BudgetVersionController(BudgetVersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public BudgetVersion createVersion(@RequestBody CreateVersionPayload payload) {
        return versionService.createVersion(payload.versionCode(), payload.name(), payload.fiscalYear());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public BudgetVersion activateVersion(@PathVariable String id) {
        return versionService.activateVersion(id);
    }

    @GetMapping("/fiscal-years/{fiscalYear}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR', 'VIEWER')")
    public List<BudgetVersion> getVersionsForYear(@PathVariable int fiscalYear) {
        return versionService.getVersionsForYear(fiscalYear);
    }

    public record CreateVersionPayload(String versionCode, String name, int fiscalYear) {
    }
}
