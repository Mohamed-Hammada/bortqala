package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialStatementsReportService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public FinancialStatementsReportService(AccountRepository accountRepository,
                                            JournalEntryRepository journalEntryRepository,
                                            JournalEntryLineRepository journalEntryLineRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    public record BalanceSheetReport(BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal totalEquity, BigDecimal netIncome, boolean balanced) {}
    public record IncomeStatementReport(BigDecimal totalRevenue, BigDecimal totalExpenses, BigDecimal netIncome) {}
    public record CashFlowReport(BigDecimal operatingCashFlow, BigDecimal investingCashFlow, BigDecimal financingCashFlow, BigDecimal netCashFlow) {}

    @Transactional(readOnly = true)
    public BalanceSheetReport getBalanceSheet(LocalDate asOfDate) {
        List<Account> accounts = accountRepository.findAll();
        List<JournalEntry> postedEntries = journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED).stream()
                .filter(je -> !je.getEntryDate().isAfter(asOfDate))
                .toList();

        Set<String> postedIds = postedEntries.stream().map(JournalEntry::getId).collect(Collectors.toSet());
        List<JournalEntryLine> lines = postedIds.isEmpty() ? List.of() : journalEntryLineRepository.findByJournalEntryIdIn(postedIds);

        Map<String, BigDecimal> accountBalances = new HashMap<>();
        for (JournalEntryLine line : lines) {
            BigDecimal current = accountBalances.getOrDefault(line.getAccountId(), BigDecimal.ZERO);
            accountBalances.put(line.getAccountId(), current.add(line.getDebit()).subtract(line.getCredit()));
        }

        BigDecimal assets = BigDecimal.ZERO;
        BigDecimal liabilities = BigDecimal.ZERO;
        BigDecimal equity = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal bal = accountBalances.getOrDefault(acc.getId(), BigDecimal.ZERO);
            if (acc.getType() == Account.Type.ASSET) {
                assets = assets.add(bal);
            } else if (acc.getType() == Account.Type.LIABILITY) {
                liabilities = liabilities.add(bal.negate());
            } else if (acc.getType() == Account.Type.EQUITY) {
                equity = equity.add(bal.negate());
            } else if (acc.getType() == Account.Type.REVENUE) {
                revenue = revenue.add(bal.negate());
            } else if (acc.getType() == Account.Type.EXPENSE) {
                expenses = expenses.add(bal);
            }
        }

        BigDecimal netIncome = revenue.subtract(expenses);
        BigDecimal totalEquityWithIncome = equity.add(netIncome);
        boolean balanced = assets.compareTo(liabilities.add(totalEquityWithIncome)) == 0;

        return new BalanceSheetReport(assets, liabilities, totalEquityWithIncome, netIncome, balanced);
    }

    @Transactional(readOnly = true)
    public IncomeStatementReport getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        List<Account> accounts = accountRepository.findAll();
        List<JournalEntry> postedEntries = journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED).stream()
                .filter(je -> !je.getEntryDate().isBefore(startDate) && !je.getEntryDate().isAfter(endDate))
                .toList();

        Set<String> postedIds = postedEntries.stream().map(JournalEntry::getId).collect(Collectors.toSet());
        List<JournalEntryLine> lines = postedIds.isEmpty() ? List.of() : journalEntryLineRepository.findByJournalEntryIdIn(postedIds);

        Map<String, BigDecimal> accountBalances = new HashMap<>();
        for (JournalEntryLine line : lines) {
            BigDecimal current = accountBalances.getOrDefault(line.getAccountId(), BigDecimal.ZERO);
            accountBalances.put(line.getAccountId(), current.add(line.getDebit()).subtract(line.getCredit()));
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal bal = accountBalances.getOrDefault(acc.getId(), BigDecimal.ZERO);
            if (acc.getType() == Account.Type.REVENUE) {
                totalRevenue = totalRevenue.add(bal.negate());
            } else if (acc.getType() == Account.Type.EXPENSE) {
                totalExpenses = totalExpenses.add(bal);
            }
        }

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        return new IncomeStatementReport(totalRevenue, totalExpenses, netIncome);
    }

    @Transactional(readOnly = true)
    public CashFlowReport getCashFlowStatement(LocalDate startDate, LocalDate endDate) {
        throw new BusinessRuleException(
                "Cash Flow Statement is unavailable until ledger-based cash classification is configured.",
                "FIN_CASH_FLOW_NOT_IMPLEMENTED",
                HttpStatus.NOT_IMPLEMENTED);
    }
}
