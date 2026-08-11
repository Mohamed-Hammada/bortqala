package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalSourceMetadataRepository extends JpaRepository<JournalSourceMetadata, String> {
    Optional<JournalSourceMetadata> findByJournalId(String journalId);
}
