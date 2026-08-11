package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TrialBalanceReportServiceTests {

    private AccountRepository accountRepository;
    private JournalEntryLineRepository journalEntryLineRepository;
    private TrialBalanceReportService trialBalanceReportService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        journalEntryLineRepository = mock(JournalEntryLineRepository.class);
        trialBalanceReportService = new TrialBalanceReportService(accountRepository, journalEntryLineRepository);
    }

    @Test
    void generatesTrialBalanceSuccessfully() {
        Account cash = new Account("1010", "Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account revenue = new Account("4010", "Sales Revenue", Account.Type.REVENUE, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, revenue));

        JournalEntryLine line1 = new JournalEntryLine("je-1", cash.getId(), null, new BigDecimal("1000.00"), BigDecimal.ZERO, "Debit cash");
        JournalEntryLine line2 = new JournalEntryLine("je-1", revenue.getId(), null, BigDecimal.ZERO, new BigDecimal("1000.00"), "Credit revenue");

        when(journalEntryLineRepository.findAll()).thenReturn(List.of(line1, line2));

        List<TrialBalanceReportService.TrialBalanceRow> tb = trialBalanceReportService.generateTrialBalance();

        assertThat(tb).hasSize(2);
        TrialBalanceReportService.TrialBalanceRow cashRow = tb.stream().filter(r -> r.accountCode().equals("1010")).findFirst().orElseThrow();
        assertThat(cashRow.periodDebit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(cashRow.closingBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));

        TrialBalanceReportService.TrialBalanceRow revenueRow = tb.stream().filter(r -> r.accountCode().equals("4010")).findFirst().orElseThrow();
        assertThat(revenueRow.periodCredit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(revenueRow.closingBalance()).isEqualByComparingTo(new BigDecimal("-1000.00"));
    }
}
