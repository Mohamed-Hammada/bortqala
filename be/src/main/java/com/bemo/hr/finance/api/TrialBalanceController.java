package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.TrialBalanceReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/reports")
public class TrialBalanceController {

    private final TrialBalanceReportService trialBalanceReportService;

    public TrialBalanceController(TrialBalanceReportService trialBalanceReportService) {
        this.trialBalanceReportService = trialBalanceReportService;
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR', 'VIEWER')")
    public List<TrialBalanceReportService.TrialBalanceRow> getTrialBalance() {
        return trialBalanceReportService.generateTrialBalance();
    }
}
