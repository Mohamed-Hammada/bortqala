package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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

    @Transactional(readOnly = true)
    public BalanceSheetReport getBalanceSheet(LocalDate asOfDate) {
        log.debug("getBalanceSheet called with asOfDate={}", asOfDate);
        List<Account> accounts = accountRepository.findAll();
        List<JournalEntry> postedEntries = journalEntryRepository.findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED, JournalEntry.Status.REVERSED)).stream()
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

        log.info("Balance sheet generated as of {}; assets={}, liabilities={}, equity={}, balanced={}", asOfDate, assets, liabilities, totalEquityWithIncome, balanced);
        return new BalanceSheetReport(assets, liabilities, totalEquityWithIncome, netIncome, balanced);
    }

    @Transactional(readOnly = true)
    public IncomeStatementReport getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        log.debug("getIncomeStatement called with startDate={}, endDate={}", startDate, endDate);
        List<Account> accounts = accountRepository.findAll();
        List<JournalEntry> postedEntries = journalEntryRepository.findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED, JournalEntry.Status.REVERSED)).stream()
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
        log.info("Income statement generated for {} to {}; revenue={}, expenses={}, netIncome={}", startDate, endDate, totalRevenue, totalExpenses, netIncome);
        return new IncomeStatementReport(totalRevenue, totalExpenses, netIncome);
    }

    @Transactional(readOnly = true)
    public CashFlowReport getCashFlowStatement(LocalDate startDate, LocalDate endDate) {
        log.debug("getCashFlowStatement called with startDate={}, endDate={}", startDate, endDate);
        List<Account> accounts = accountRepository.findAll();
        Map<String, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));

        IncomeStatementReport is = getIncomeStatement(startDate, endDate);
        BigDecimal netIncome = is.netIncome();

        // Calculate opening cash (cash/bank accounts before startDate)
        List<JournalEntry> priorEntries = journalEntryRepository.findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED, JournalEntry.Status.REVERSED)).stream()
                .filter(je -> je.getEntryDate().isBefore(startDate))
                .toList();
        Set<String> priorIds = priorEntries.stream().map(JournalEntry::getId).collect(Collectors.toSet());
        List<JournalEntryLine> priorLines = priorIds.isEmpty() ? List.of() : journalEntryLineRepository.findByJournalEntryIdIn(priorIds);

        BigDecimal openingCash = BigDecimal.ZERO;
        for (JournalEntryLine line : priorLines) {
            Account acc = accountMap.get(line.getAccountId());
            if (acc != null && isCashOrBank(acc)) {
                openingCash = openingCash.add(line.getDebit()).subtract(line.getCredit());
            }
        }

        // Lines in current period
        List<JournalEntry> currentEntries = journalEntryRepository.findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED, JournalEntry.Status.REVERSED)).stream()
                .filter(je -> !je.getEntryDate().isBefore(startDate) && !je.getEntryDate().isAfter(endDate))
                .toList();
        Set<String> currentIds = currentEntries.stream().map(JournalEntry::getId).collect(Collectors.toSet());
        List<JournalEntryLine> currentLines = currentIds.isEmpty() ? List.of() : journalEntryLineRepository.findByJournalEntryIdIn(currentIds);

        BigDecimal operatingCash = netIncome;
        BigDecimal investingCash = BigDecimal.ZERO;
        BigDecimal financingCash = BigDecimal.ZERO;

        for (JournalEntryLine line : currentLines) {
            Account acc = accountMap.get(line.getAccountId());
            if (acc == null) continue;
            BigDecimal movement = line.getCredit().subtract(line.getDebit());

            if (acc.getType() == Account.Type.ASSET && !isCashOrBank(acc)) {
                if (acc.getCode().startsWith("12") || acc.getName().toLowerCase().contains("fixed") || acc.getName().contains("أصول ثابتة")) {
                    investingCash = investingCash.add(movement);
                } else {
                    operatingCash = operatingCash.add(movement);
                }
            } else if (acc.getType() == Account.Type.LIABILITY) {
                if (acc.getCode().startsWith("22") || acc.getName().toLowerCase().contains("loan") || acc.getName().contains("قرض")) {
                    financingCash = financingCash.add(movement.negate());
                } else {
                    operatingCash = operatingCash.add(movement.negate());
                }
            } else if (acc.getType() == Account.Type.EQUITY) {
                financingCash = financingCash.add(movement.negate());
            }
        }

        BigDecimal netCashFlow = operatingCash.add(investingCash).add(financingCash);
        BigDecimal closingCash = openingCash.add(netCashFlow);

        log.info("Cash flow generated: operating={}, investing={}, financing={}, net={}, opening={}, closing={}",
                operatingCash, investingCash, financingCash, netCashFlow, openingCash, closingCash);

        return new CashFlowReport(operatingCash, investingCash, financingCash, netCashFlow, openingCash, closingCash);
    }

    private boolean isCashOrBank(Account acc) {
        String code = acc.getCode() != null ? acc.getCode() : "";
        String name = acc.getName() != null ? acc.getName().toLowerCase() : "";
        return code.startsWith("10") || code.startsWith("11") || name.contains("cash") || name.contains("bank") || name.contains("صندوق") || name.contains("بنك") || name.contains("نقدية");
    }

    public record BalanceSheetReport(BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal totalEquity,
                                     BigDecimal netIncome, boolean balanced) {
    }

    public record IncomeStatementReport(BigDecimal totalRevenue, BigDecimal totalExpenses, BigDecimal netIncome) {
    }

    public record CashFlowReport(BigDecimal operatingCashFlow, BigDecimal investingCashFlow,
                                 BigDecimal financingCashFlow, BigDecimal netCashFlow,
                                 BigDecimal openingCashBalance, BigDecimal closingCashBalance) {
    }
}
