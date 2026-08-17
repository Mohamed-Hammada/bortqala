package com.bemo.hr.shared.numbering;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentNumberServiceTests {

    @Mock
    private DocumentNumberSequenceRepository sequenceRepository;

    private DocumentNumberService service;

    @BeforeEach
    void setUp() {
        service = new DocumentNumberService(sequenceRepository);
    }

    @Test
    void formatsPrefixYearAndZeroPaddedSequence() {
        when(sequenceRepository.findByDocumentTypeAndYear("JOURNAL_ENTRY", 2026))
                .thenReturn(Optional.of(new DocumentNumberSequence("JOURNAL_ENTRY", 2026, 1)));
        assertThat(service.next("JOURNAL_ENTRY", "JV", LocalDate.of(2026, 8, 6)))
                .isEqualTo("JV-2026-00001");
    }

    @Test
    void incrementsSequentiallyFromExistingRow() {
        DocumentNumberSequence seq = new DocumentNumberSequence("JOURNAL_ENTRY", 2026, 7);
        when(sequenceRepository.findByDocumentTypeAndYear("JOURNAL_ENTRY", 2026)).thenReturn(Optional.of(seq));
        assertThat(service.next("JOURNAL_ENTRY", "JV", LocalDate.of(2026, 8, 6)))
                .isEqualTo("JV-2026-00007");
        assertThat(service.next("JOURNAL_ENTRY", "JV", LocalDate.of(2026, 8, 6)))
                .isEqualTo("JV-2026-00008");
    }

    @Test
    void createsFreshRowForUnknownTypeAndYear() {
        when(sequenceRepository.findByDocumentTypeAndYear(anyString(), anyInt())).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(DocumentNumberSequence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.next("SUPPLIER_PAYMENT", "PMT", LocalDate.of(2026, 8, 6)))
                .isEqualTo("PMT-2026-00001");
        verify(sequenceRepository).save(any(DocumentNumberSequence.class));
    }

    @Test
    void rollsOverToNewYearWithFreshSequence() {
        when(sequenceRepository.findByDocumentTypeAndYear("JOURNAL_ENTRY", 2026))
                .thenReturn(Optional.of(new DocumentNumberSequence("JOURNAL_ENTRY", 2026, 9)));
        when(sequenceRepository.findByDocumentTypeAndYear("JOURNAL_ENTRY", 2027)).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(DocumentNumberSequence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.next("JOURNAL_ENTRY", "JV", LocalDate.of(2026, 12, 31)))
                .isEqualTo("JV-2026-00009");
        assertThat(service.next("JOURNAL_ENTRY", "JV", LocalDate.of(2027, 1, 1)))
                .isEqualTo("JV-2027-00001");
    }
}
