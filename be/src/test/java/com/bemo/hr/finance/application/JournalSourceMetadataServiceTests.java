package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import com.bemo.hr.finance.infrastructure.JournalSourceMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalSourceMetadataServiceTests {

    private JournalSourceMetadataRepository repository;
    private JournalSourceMetadataService service;

    @BeforeEach
    void setUp() {
        repository = mock(JournalSourceMetadataRepository.class);
        service = new JournalSourceMetadataService(repository);
    }

    @Test
    void attachesSourceMetadataAndEnforcesImmutableLockSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JournalSourceMetadata metadata = service.attachSourceMetadata("journal-999", "INVOICE", "inv-100");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getSourceDocumentType()).isEqualTo("INVOICE");
        assertThat(metadata.getSourceDocumentId()).isEqualTo("inv-100");
        assertThat(metadata.isImmutableLock()).isTrue();

        when(repository.findByJournalId("journal-999")).thenReturn(Optional.of(metadata));
        assertThat(service.getMetadata("journal-999")).isNotNull();
    }
}
