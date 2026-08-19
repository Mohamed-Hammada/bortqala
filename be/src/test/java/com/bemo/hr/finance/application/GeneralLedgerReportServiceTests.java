package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneralLedgerReportServiceTests {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;
    @Mock
    private AccountRepository accountRepository;

    private GeneralLedgerReportService glService;

    @BeforeEach
    void setUp() {
        glService = new GeneralLedgerReportService(journalEntryRepository, journalEntryLineRepository, accountRepository);
    }

    @Test
    @DisplayName("Filters general ledger lines by projectId and costCodeId")
    void testDetailWithProjectFilter() {
        Account cash = mock(Account.class);
        when(cash.getId()).thenReturn("acc-1");
        when(cash.getCode()).thenReturn("1010");
        when(accountRepository.findAll()).thenReturn(List.of(cash));

        JournalEntry entry1 = new JournalEntry("JV-001", LocalDate.of(2026, 8, 1), "Project Site Expense", "REF-1", null);
        entry1.assignProject("proj-101", "wbs-site", "CC-MAT");
        JournalEntry entry2 = new JournalEntry("JV-002", LocalDate.of(2026, 8, 2), "HQ Overhead", "REF-2", null);

        JournalEntryLine line1 = new JournalEntryLine(entry1.getId(), "acc-1", null, BigDecimal.valueOf(5000), BigDecimal.ZERO, "Site Materials");
        line1.assignProject("proj-101", "wbs-site", "CC-MAT");

        JournalEntryLine line2 = new JournalEntryLine(entry2.getId(), "acc-1", null, BigDecimal.valueOf(2000), BigDecimal.ZERO, "Office supplies");

        when(journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED))
                .thenReturn(List.of(entry1, entry2));
        when(journalEntryLineRepository.findByJournalEntryId(entry1.getId())).thenReturn(List.of(line1));
        when(journalEntryLineRepository.findByJournalEntryId(entry2.getId())).thenReturn(List.of(line2));

        // When querying all projects
        List<GeneralLedgerReportService.Row> allRows = glService.detail(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null, null);
        assertThat(allRows).hasSize(2);

        // When filtering by proj-101
        List<GeneralLedgerReportService.Row> projectRows = glService.detail(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "proj-101", null);
        assertThat(projectRows).hasSize(1);
        assertThat(projectRows.get(0).entryNumber()).isEqualTo("JV-001");
        assertThat(projectRows.get(0).projectId()).isEqualTo("proj-101");
        assertThat(projectRows.get(0).costCodeId()).isEqualTo("CC-MAT");
        assertThat(projectRows.get(0).debit()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }
}
