package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.FinancialStatementsReportService;
import com.bemo.hr.finance.application.FinancialStatementsReportService.BalanceSheetReport;
import com.bemo.hr.finance.application.FinancialStatementsReportService.CashFlowReport;
import com.bemo.hr.finance.application.FinancialStatementsReportService.IncomeStatementReport;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finance/reports")
public class FinancialStatementsController {

    private final FinancialStatementsReportService statementsService;

    public FinancialStatementsController(FinancialStatementsReportService statementsService) {
        this.statementsService = statementsService;
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.AUDITOR + " or " + Roles.FINANCE_MANAGER)
    public BalanceSheetReport getBalanceSheet(@RequestParam String asOfDate) {
        return statementsService.getBalanceSheet(LocalDate.parse(asOfDate));
    }

    @GetMapping("/income-statement")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.AUDITOR + " or " + Roles.FINANCE_MANAGER)
    public IncomeStatementReport getIncomeStatement(@RequestParam String startDate, @RequestParam String endDate) {
        return statementsService.getIncomeStatement(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.AUDITOR + " or " + Roles.FINANCE_MANAGER)
    public CashFlowReport getCashFlowStatement(@RequestParam String startDate, @RequestParam String endDate) {
        return statementsService.getCashFlowStatement(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
}
