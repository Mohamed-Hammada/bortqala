package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.posting.JournalDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalDimensionRepository extends JpaRepository<JournalDimension, String> {
    Optional<JournalDimension> findByJournalEntryLineId(String journalEntryLineId);

    List<JournalDimension> findByJournalEntryLineIdIn(Collection<String> journalEntryLineIds);
}
