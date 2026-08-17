package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkforceAdvanceLedgerEntryRepository extends JpaRepository<WorkforceAdvanceLedgerEntry, String> {
    List<WorkforceAdvanceLedgerEntry> findByAdvanceId(String advanceId);
}
