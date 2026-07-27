package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, String> {
    List<JournalEntryLine> findByJournalEntryId(String journalEntryId);
    void deleteByJournalEntryId(String journalEntryId);
}
