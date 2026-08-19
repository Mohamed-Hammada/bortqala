package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.TrialBalanceReportService;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/reports")
public class TrialBalanceController {

    private final TrialBalanceReportService trialBalanceReportService;

    public TrialBalanceController(TrialBalanceReportService trialBalanceReportService) {
        this.trialBalanceReportService = trialBalanceReportService;
    }

    @GetMapping("/trial-balance")
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_AUDITOR_FINANCE_MANAGER_VIEWER)
    public List<TrialBalanceReportService.TrialBalanceRow> getTrialBalance(
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        return trialBalanceReportService.generateTrialBalance(from == null ? LocalDate.MIN : from, to == null ? LocalDate.MAX : to);
    }
}
