package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.FinancialStatementsReportService;
import com.bemo.hr.finance.application.FinancialStatementsReportService.BalanceSheetReport;
import com.bemo.hr.finance.application.FinancialStatementsReportService.CashFlowReport;
import com.bemo.hr.finance.application.FinancialStatementsReportService.IncomeStatementReport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finance/reports")
public class FinancialStatementsController {

    private final FinancialStatementsReportService statementsService;

    public FinancialStatementsController(FinancialStatementsReportService statementsService) {
        this.statementsService = statementsService;
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public BalanceSheetReport getBalanceSheet(@RequestParam String asOfDate) {
        return statementsService.getBalanceSheet(LocalDate.parse(asOfDate));
    }

    @GetMapping("/income-statement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public IncomeStatementReport getIncomeStatement(@RequestParam String startDate, @RequestParam String endDate) {
        return statementsService.getIncomeStatement(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public CashFlowReport getCashFlowStatement(@RequestParam String startDate, @RequestParam String endDate) {
        return statementsService.getCashFlowStatement(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
}
