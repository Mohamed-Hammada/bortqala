package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import com.bemo.hr.finance.infrastructure.JournalSourceMetadataRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalSourceMetadataService {

    private final JournalSourceMetadataRepository repository;

    public JournalSourceMetadataService(JournalSourceMetadataRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public JournalSourceMetadata attachSourceMetadata(String journalId, String sourceDocumentType, String sourceDocumentId) {
        JournalSourceMetadata metadata = new JournalSourceMetadata(journalId, sourceDocumentType, sourceDocumentId);
        return repository.save(metadata);
    }

    @Transactional(readOnly = true)
    public JournalSourceMetadata getMetadata(String journalId) {
        return repository.findByJournalId(journalId)
                .orElseThrow(() -> new BusinessRuleException("Journal source metadata not found", "JOURNAL_METADATA_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
