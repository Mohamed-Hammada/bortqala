package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.BankAccount;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.domain.treasury.Cashbox;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Proves TreasuryPositionService returns real, ledger-derived balances rather than the arbitrary
 * ratios (revenue*0.40, count*450000, ...) that ExecutiveAnalyticsService and
 * ProjectExecutiveDashboardService fabricated before the 2026-09-06 remediation.
 */
@ExtendWith(MockitoExtension.class)
class TreasuryPositionServiceTests {

    @Mock
    private CashboxRepository cashboxRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;

    private TreasuryPositionService service;

    @BeforeEach
    void setUp() {
        service = new TreasuryPositionService(cashboxRepository, bankAccountRepository,
                journalEntryRepository, journalEntryLineRepository);
    }

    @Test
    void totalCashBalanceSumsRealCashboxBalancesNotAFakeConstant() {
        Cashbox main = new Cashbox("CASH-01", "Main Safe", "branch-1", "EGP", "user-1", "acc-1");
        main.adjustBalance(BigDecimal.valueOf(37500));
        Cashbox petty = new Cashbox("CASH-02", "Petty Cash", "branch-2", "EGP", "user-2", "acc-2");
        petty.adjustBalance(BigDecimal.valueOf(1250));
        when(cashboxRepository.findAll()).thenReturn(List.of(main, petty));

        BigDecimal total = service.totalCashBalance();

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(38750));
    }

    @Test
    void totalCashBalanceIsZeroWhenNoCashboxesExist() {
        when(cashboxRepository.findAll()).thenReturn(List.of());

        assertThat(service.totalCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void totalBankBalanceSumsRealGlAccountBalancesForLinkedAccounts() {
        BankAccount linked = new BankAccount("NBE", "100200300", null, null, "gl-acc-9", "EGP", "branch-1", true);
        BankAccount unlinked = new BankAccount("CIB", "900800700", null, null, null, "EGP", "branch-2", true);
        when(bankAccountRepository.findAll()).thenReturn(List.of(linked, unlinked));

        JournalEntry posted = new JournalEntry("JV-1", java.time.LocalDate.now(), "desc", "ref", "period-1");
        when(journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED))
                .thenReturn(List.of(posted));
        JournalEntryLine debitLine = new JournalEntryLine(posted.getId(), "gl-acc-9", null,
                BigDecimal.valueOf(90000), BigDecimal.ZERO, "deposit");
        JournalEntryLine creditLine = new JournalEntryLine(posted.getId(), "gl-acc-9", null,
                BigDecimal.ZERO, BigDecimal.valueOf(15000), "withdrawal");
        when(journalEntryLineRepository.findByJournalEntryIdIn(List.of(posted.getId())))
                .thenReturn(List.of(debitLine, creditLine));

        BigDecimal total = service.totalBankBalance();

        // Only the linked account contributes (90000 - 15000 = 75000); the unlinked account contributes 0,
        // not a guessed amount.
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(75000));
    }

    @Test
    void totalBankBalanceIsZeroWhenThereAreNoPostedJournalEntries() {
        BankAccount linked = new BankAccount("NBE", "100200300", null, null, "gl-acc-9", "EGP", "branch-1", true);
        when(bankAccountRepository.findAll()).thenReturn(List.of(linked));
        when(journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED))
                .thenReturn(List.of());

        assertThat(service.totalBankBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cashBalanceByBranchGroupsRealBalancesByBranchId() {
        Cashbox branch1 = new Cashbox("CASH-01", "Branch 1 Safe", "branch-1", "EGP", "user-1", "acc-1");
        branch1.adjustBalance(BigDecimal.valueOf(20000));
        Cashbox branch2 = new Cashbox("CASH-02", "Branch 2 Safe", "branch-2", "EGP", "user-2", "acc-2");
        branch2.adjustBalance(BigDecimal.valueOf(5000));
        when(cashboxRepository.findAll()).thenReturn(List.of(branch1, branch2));

        Map<String, BigDecimal> byBranch = service.cashBalanceByBranch();

        assertThat(byBranch.get("branch-1")).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(byBranch.get("branch-2")).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }
}
