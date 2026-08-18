package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import com.bemo.hr.finance.infrastructure.JournalSourceMetadataRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JournalSourceMetadataService {

    private final JournalSourceMetadataRepository repository;

    public JournalSourceMetadataService(JournalSourceMetadataRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public JournalSourceMetadata attachSourceMetadata(String journalId, String sourceDocumentType, String sourceDocumentId) {
        log.debug("attachSourceMetadata called with journalId={}, sourceDocumentType={}, sourceDocumentId={}", journalId, sourceDocumentType, sourceDocumentId);
        JournalSourceMetadata metadata = new JournalSourceMetadata(journalId, sourceDocumentType, sourceDocumentId);
        JournalSourceMetadata saved = repository.save(metadata);
        log.info("JournalSourceMetadata attached to journal {} for document {}/{}", journalId, sourceDocumentType, sourceDocumentId);
        return saved;
    }

    @Transactional(readOnly = true)
    public JournalSourceMetadata getMetadata(String journalId) {
        return repository.findByJournalId(journalId)
                .orElseThrow(() -> new BusinessRuleException("Journal source metadata not found", "JOURNAL_METADATA_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
