package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

class FinancialStatementsReportServiceTests {

    private AccountRepository accountRepository;
    private JournalEntryRepository journalEntryRepository;
    private JournalEntryLineRepository journalEntryLineRepository;
    private FinancialStatementsReportService statementsService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        journalEntryRepository = mock(JournalEntryRepository.class);
        journalEntryLineRepository = mock(JournalEntryLineRepository.class);
        statementsService = new FinancialStatementsReportService(accountRepository, journalEntryRepository, journalEntryLineRepository);
    }

    @Test
    void generatesBalanceSheetAndIncomeStatementSuccessfully() {
        Account cash = new Account("1010", "Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account revenue = new Account("4010", "Sales Revenue", Account.Type.REVENUE, null, false, "EGP", true);
        Account expense = new Account("5010", "Rent Expense", Account.Type.EXPENSE, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, revenue, expense));

        JournalEntry je = new JournalEntry("JE-2026-001", LocalDate.of(2026, 3, 1), "Sale and Rent", "REF-01", "FP-01");
        je.approve("manager");
        je.post("admin");
        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(anyList())).thenReturn(List.of(je));

        JournalEntryLine l1 = new JournalEntryLine(je.getId(), cash.getId(), null, new BigDecimal("1000.00"), BigDecimal.ZERO, "Cash in");
        JournalEntryLine l2 = new JournalEntryLine(je.getId(), revenue.getId(), null, BigDecimal.ZERO, new BigDecimal("1000.00"), "Revenue");
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenReturn(List.of(l1, l2));

        FinancialStatementsReportService.BalanceSheetReport bs = statementsService.getBalanceSheet(LocalDate.of(2026, 3, 31));
        assertThat(bs.totalAssets()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(bs.netIncome()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(bs.balanced()).isTrue();

        FinancialStatementsReportService.IncomeStatementReport inc = statementsService.getIncomeStatement(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat(inc.totalRevenue()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(inc.netIncome()).isEqualByComparingTo(new BigDecimal("1000.00"));

    }

    @Test
    void cashFlowIsExplicitlyUnavailableUntilLedgerClassificationExists() {
        assertThatThrownBy(() -> statementsService.getCashFlowStatement(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .satisfies(error -> {
                    var businessError = (com.bemo.hr.shared.domain.BusinessRuleException) error;
                    assertThat(businessError.getCode()).isEqualTo("FIN_CASH_FLOW_NOT_IMPLEMENTED");
                    assertThat(businessError.getStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_IMPLEMENTED);
                });
    }
}
