package com.bemo.hr.finance.domain.posting;

import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubledgerPostingServiceTests {

    private PostingProfileRepository postingProfileRepository;
    private JournalEntryRepository journalEntryRepository;
    private JournalEntryLineRepository journalEntryLineRepository;
    private SubledgerPostingService subledgerPostingService;

    @BeforeEach
    void setUp() {
        postingProfileRepository = mock(PostingProfileRepository.class);
        journalEntryRepository = mock(JournalEntryRepository.class);
        journalEntryLineRepository = mock(JournalEntryLineRepository.class);
        subledgerPostingService = new SubledgerPostingService(postingProfileRepository, journalEntryRepository, journalEntryLineRepository);
    }

    @Test
    void postsBalancedSubledgerEventSuccessfully() {
        String opId = UUID.randomUUID().toString();
        PostingProfile profile = new PostingProfile("P01", "CONTRACTOR_SETTLEMENT_POST", LocalDate.of(2026, 1, 1), null);
        when(postingProfileRepository.findByBusinessEventAndActiveTrue("CONTRACTOR_SETTLEMENT_POST"))
                .thenReturn(List.of(profile));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalEntry entry = subledgerPostingService.postSubledgerEvent(
                "WORKFORCE",
                "SETTLEMENT",
                "SETTLE-100",
                "CONTRACTOR_SETTLEMENT_POST",
                opId,
                LocalDate.of(2026, 2, 1),
                "Contractor Settlement Posting",
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                "fp-1"
        );

        assertThat(entry).isNotNull();
        assertThat(entry.getStatus()).isEqualTo(JournalEntry.Status.POSTED);
        assertThat(entry.getOperationId()).isEqualTo(opId);
        assertThat(entry.getReference()).isEqualTo("WORKFORCE:SETTLEMENT:SETTLE-100");
        verify(journalEntryLineRepository, times(2)).save(any(JournalEntryLine.class));
    }

    @Test
    void rejectsUnbalancedSubledgerPosting() {
        String opId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> subledgerPostingService.postSubledgerEvent(
                "WORKFORCE",
                "SETTLEMENT",
                "SETTLE-100",
                "CONTRACTOR_SETTLEMENT_POST",
                opId,
                LocalDate.of(2026, 2, 1),
                "Contractor Settlement Posting",
                new BigDecimal("5000.00"),
                new BigDecimal("4500.00"), // Unbalanced
                "fp-1"
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Debit and credit amounts must be equal");
    }
}
