package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.BankReconciliationMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankReconciliationMatchRepository extends JpaRepository<BankReconciliationMatch, String> {
    List<BankReconciliationMatch> findByStatementLineIdOrderByMatchedAtAsc(String lineId);

    List<BankReconciliationMatch> findByOperationId(String operationId);

    boolean existsByJournalEntryIdAndStatus(String journalEntryId, BankReconciliationMatch.Status status);

    List<BankReconciliationMatch> findByJournalEntryIdAndStatus(String journalEntryId, BankReconciliationMatch.Status status);
}
