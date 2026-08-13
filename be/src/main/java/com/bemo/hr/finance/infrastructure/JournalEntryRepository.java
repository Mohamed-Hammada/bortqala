package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {
    Page<JournalEntry> findAllByOrderByEntryDateDescCreatedAtDesc(Pageable pageable);
    List<JournalEntry> findByStatusOrderByEntryDateDesc(JournalEntry.Status status);
    boolean existsByAppIdAndEntryNumber(String appId, String entryNumber);
    boolean existsByAppIdAndEntryNumberAndIdNot(String appId, String entryNumber, String id);
    long countByFiscalPeriodIdAndStatus(String fiscalPeriodId, JournalEntry.Status status);
    Optional<JournalEntry> findByOperationId(String operationId);
}
