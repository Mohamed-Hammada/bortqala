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

    /**
     * Direct-method cash flow statement derived exclusively from posted GL evidence.
     *
     * For every posted (or reversed-with-its-offsetting-reversal) journal entry that actually
     * moves a cash/bank account, the entry's net cash movement is attributed to operating,
     * investing, or financing according to the counter-accounts of the same entry:
     * - financing: equity accounts and long-term loan liabilities (code prefix 22 / loan naming);
     * - investing: non-current asset acquisitions/disposals (fixed-asset code prefix 12);
     * - operating: every other counter-account (trade, revenue, expense, short-term items).
     * Entries without any cash movement are accrual-only and never affect the statement, so
     * opening cash + operating + investing + financing always reconciles exactly to closing cash.
     */
    @Transactional(readOnly = true)
    public CashFlowReport getCashFlowStatement(LocalDate startDate, LocalDate endDate) {
        log.debug("getCashFlowStatement called with startDate={}, endDate={}", startDate, endDate);
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Cash flow end date must not be before start date");
        }

        CashFlowPeriod current = computeCashFlowPeriod(startDate, endDate);
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate comparativeStart = startDate.minusDays(periodDays);
        LocalDate comparativeEnd = startDate.minusDays(1);
        CashFlowPeriod previous = computeCashFlowPeriod(comparativeStart, comparativeEnd);

        BigDecimal closingCash = cashBalanceOnOrBefore(endDate);
        boolean reconciled = current.opening().add(current.operating()).add(current.investing())
                .add(current.financing()).compareTo(closingCash) == 0;

        PeriodComparison comparison = new PeriodComparison(
                comparativeStart, comparativeEnd,
                previous.operating(), previous.investing(), previous.financing(),
                previous.operating().add(previous.investing()).add(previous.financing()));

        log.info("Direct cash flow generated {}..{}: operating={}, investing={}, financing={}, "
                        + "opening={}, closing={}, reconciled={}",
                startDate, endDate, current.operating(), current.investing(), current.financing(),
                current.opening(), closingCash, reconciled);

        return new CashFlowReport(current.operating(), current.investing(), current.financing(),
                current.operating().add(current.investing()).add(current.financing()),
                current.opening(), closingCash, reconciled, comparison);
    }

    private record CashFlowPeriod(BigDecimal opening, BigDecimal operating,
                                  BigDecimal investing, BigDecimal financing) {
    }

    private CashFlowPeriod computeCashFlowPeriod(LocalDate startDate, LocalDate endDate) {
        Map<String, Account> accountMap = accountMap();
        List<JournalEntryLine> lines = postedLinesInRange(startDate, endDate);

        BigDecimal openingCash = cashBalanceOnOrBefore(startDate.minusDays(1));
        BigDecimal operating = BigDecimal.ZERO;
        BigDecimal investing = BigDecimal.ZERO;
        BigDecimal financing = BigDecimal.ZERO;

        Map<String, List<JournalEntryLine>> linesByEntry = lines.stream()
                .collect(Collectors.groupingBy(JournalEntryLine::getJournalEntryId));
        for (List<JournalEntryLine> entryLines : linesByEntry.values()) {
            BigDecimal cashDelta = BigDecimal.ZERO;
            boolean movesCash = false;
            for (JournalEntryLine line : entryLines) {
                Account acc = accountMap.get(line.getAccountId());
                if (acc == null || !isCashOrBank(acc)) continue;
                movesCash = true;
                cashDelta = cashDelta.add(line.getDebit().subtract(line.getCredit()));
            }
            if (!movesCash) {
                continue; // accrual-only entries never move cash
            }
            for (JournalEntryLine line : entryLines) {
                Account acc = accountMap.get(line.getAccountId());
                if (acc == null || isCashOrBank(acc)) continue;
                // Balanced-entry identity: sum of counter movements equals the entry's cash delta.
                BigDecimal counterMovement = line.getCredit().subtract(line.getDebit());
                if (isFinancingCounterpart(acc)) {
                    financing = financing.add(counterMovement);
                } else if (isInvestingCounterpart(acc)) {
                    investing = investing.add(counterMovement);
                } else {
                    operating = operating.add(counterMovement);
                }
            }
        }
        return new CashFlowPeriod(openingCash, operating, investing, financing);
    }

    private BigDecimal cashBalanceOnOrBefore(LocalDate date) {
        Map<String, Account> accountMap = accountMap();
        List<JournalEntry> priorEntries = journalEntryRepository
                .findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED, JournalEntry.Status.REVERSED))
                .stream()
                .filter(je -> !je.getEntryDate().isAfter(date))
                .toList();
        Set<String> priorIds = priorEntries.stream().map(JournalEntry::getId).collect(Collectors.toSet());
        List<JournalEntryLine> priorLines = priorIds.isEmpty() ? List.of()
                : journalEntryLineRepository.findByJournalEntryIdIn(priorIds);
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalEntryLine line : priorLines) {
            Account acc = accountMap.get(line.getAccountId());
            if (acc != null && isCashOrBank(acc)) {
                balance = balance.add(line.getDebit()).subtract(line.getCredit());
            }
        }
        return balance;
    }

    private List<JournalEntryLine> postedLinesInRange(LocalDate startDate, LocalDate endDate) {
        List<JournalEntry> entries = journalEntryRepository
                .findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED, JournalEntry.Status.REVERSED))
                .stream()
                .filter(je -> !je.getEntryDate().isBefore(startDate) && !je.getEntryDate().isAfter(endDate))
                .toList();
        Set<String> ids = entries.stream().map(JournalEntry::getId).collect(Collectors.toSet());
        return ids.isEmpty() ? List.of() : journalEntryLineRepository.findByJournalEntryIdIn(ids);
    }

    private Map<String, Account> accountMap() {
        return accountRepository.findAll().stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
    }

    /** Equity accounts and long-term loan liabilities are financing activities. */
    private boolean isFinancingCounterpart(Account acc) {
        if (acc.getType() == Account.Type.EQUITY) {
            return true;
        }
        if (acc.getType() == Account.Type.LIABILITY) {
            String code = acc.getCode() == null ? "" : acc.getCode();
            String name = acc.getName() == null ? "" : acc.getName().toLowerCase();
            return code.startsWith("22") || name.contains("loan") || name.contains("قرض");
        }
        return false;
    }

    /** Non-current asset acquisitions/disposals (fixed-asset chart prefix 12) are investing activities. */
    private boolean isInvestingCounterpart(Account acc) {
        String code = acc.getCode() == null ? "" : acc.getCode();
        return acc.getType() == Account.Type.ASSET && !isCashOrBank(acc) && code.startsWith("12");
    }

    private boolean isCashOrBank(Account acc) {
        if (acc.getType() != Account.Type.ASSET) {
            // Liability/expense accounts named "Bank Loan" or "Bank Fees" are never cash pool members.
            return false;
        }
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
                                 BigDecimal openingCashBalance, BigDecimal closingCashBalance,
                                 boolean reconciled, PeriodComparison comparative) {
    }

    public record PeriodComparison(LocalDate startDate, LocalDate endDate,
                                   BigDecimal operatingCashFlow, BigDecimal investingCashFlow,
                                   BigDecimal financingCashFlow, BigDecimal netCashFlow) {
    }
}
