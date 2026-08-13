package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CloseChecklistServiceTests {

    private FiscalPeriodRepository fiscalPeriodRepository;
    private JournalEntryRepository journalEntryRepository;
    private CloseChecklistService closeChecklistService;

    @BeforeEach
    void setUp() {
        fiscalPeriodRepository = mock(FiscalPeriodRepository.class);
        journalEntryRepository = mock(JournalEntryRepository.class);
        closeChecklistService = new CloseChecklistService(fiscalPeriodRepository, journalEntryRepository, java.util.List.of());
    }

    @Test
    void allowsCloseWhenNoBlockersExist() {
        FiscalPeriod period = new FiscalPeriod(2026, 1, "Jan 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), FiscalPeriod.Status.OPEN);
        when(fiscalPeriodRepository.findById("p-1")).thenReturn(Optional.of(period));
        when(journalEntryRepository.countByFiscalPeriodIdAndStatus("p-1", com.bemo.hr.finance.domain.JournalEntry.Status.DRAFT)).thenReturn(0L);

        CloseChecklistSummary summary = closeChecklistService.computePrecheck("p-1");

        assertThat(summary.canClose()).isTrue();
        assertThat(summary.checks()).extracting(CloseCheckItem::severity).doesNotContain(CloseCheckItem.Severity.BLOCKER);
    }

    @Test
    void blocksCloseWhenDraftJournalsExist() {
        FiscalPeriod period = new FiscalPeriod(2026, 1, "Jan 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), FiscalPeriod.Status.OPEN);
        when(fiscalPeriodRepository.findById("p-1")).thenReturn(Optional.of(period));
        when(journalEntryRepository.countByFiscalPeriodIdAndStatus("p-1", com.bemo.hr.finance.domain.JournalEntry.Status.DRAFT)).thenReturn(3L);

        CloseChecklistSummary summary = closeChecklistService.computePrecheck("p-1");

        assertThat(summary.canClose()).isFalse();
        assertThat(summary.checks()).filteredOn(c -> c.code().equals("GL_DRAFT_JOURNALS"))
                .extracting(CloseCheckItem::severity)
                .containsExactly(CloseCheckItem.Severity.BLOCKER);
    }
}
