package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.BankAccount;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.domain.treasury.Cashbox;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real cash-in-hand and bank-balance figures, shared by every dashboard/cockpit that needs a
 * tenant- or branch-level treasury position. Extracted 2026-09-06 to stop {@code
 * ExecutiveAnalyticsService} and {@code ProjectExecutiveDashboardService} each fabricating their
 * own bank/cash numbers with arbitrary ratios (see docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md,
 * Critical Finding C-1) — both now call this instead.
 *
 * Cashbox balances are real, tenant-maintained running balances ({@link Cashbox#getCurrentBalance()}).
 * Bank-account balances have no stored balance field on {@link BankAccount} itself; each account
 * that is linked to a posting GL account ({@link BankAccount#getAccountId()}) gets its real balance
 * from posted journal-entry lines against that account, mirroring the same debit-minus-credit
 * pattern {@code InventoryValuationService}/{@code FinancialStatementsReportService} already use
 * for other GL account balances. A bank account with no linked GL account contributes zero — never
 * a guessed amount.
 */
@Service
public class TreasuryPositionService {

    private final CashboxRepository cashboxRepository;
    private final BankAccountRepository bankAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public TreasuryPositionService(CashboxRepository cashboxRepository,
                                    BankAccountRepository bankAccountRepository,
                                    JournalEntryRepository journalEntryRepository,
                                    JournalEntryLineRepository journalEntryLineRepository) {
        this.cashboxRepository = cashboxRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalCashBalance() {
        return cashboxRepository.findAll().stream()
                .map(c -> c.getCurrentBalance() != null ? c.getCurrentBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> cashBalanceByBranch() {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Cashbox c : cashboxRepository.findAll()) {
            if (c.getBranchId() == null) continue;
            BigDecimal balance = c.getCurrentBalance() != null ? c.getCurrentBalance() : BigDecimal.ZERO;
            result.merge(c.getBranchId(), balance, BigDecimal::add);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalBankBalance() {
        List<BankAccount> accounts = bankAccountRepository.findAll();
        if (accounts.isEmpty()) return BigDecimal.ZERO;
        Map<String, BigDecimal> glBalances = glAccountBalances();
        return accounts.stream()
                .map(a -> a.getAccountId() != null ? glBalances.getOrDefault(a.getAccountId(), BigDecimal.ZERO) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> bankBalanceByBranch() {
        List<BankAccount> accounts = bankAccountRepository.findAll();
        Map<String, BigDecimal> glBalances = glAccountBalances();
        Map<String, BigDecimal> result = new HashMap<>();
        for (BankAccount a : accounts) {
            if (a.getBranchId() == null || a.getAccountId() == null) continue;
            BigDecimal balance = glBalances.getOrDefault(a.getAccountId(), BigDecimal.ZERO);
            result.merge(a.getBranchId(), balance, BigDecimal::add);
        }
        return result;
    }

    /** Real GL balance (debit - credit over posted lines) per account id, computed once per call. */
    private Map<String, BigDecimal> glAccountBalances() {
        List<String> postedIds = journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED)
                .stream().map(JournalEntry::getId).toList();
        if (postedIds.isEmpty()) return Map.of();
        Map<String, BigDecimal> balances = new HashMap<>();
        for (JournalEntryLine line : journalEntryLineRepository.findByJournalEntryIdIn(postedIds)) {
            BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
            balances.merge(line.getAccountId(), debit.subtract(credit), BigDecimal::add);
        }
        return balances;
    }
}
