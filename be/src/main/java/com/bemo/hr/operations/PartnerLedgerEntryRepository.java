package com.bemo.hr.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface PartnerLedgerEntryRepository extends JpaRepository<PartnerLedgerEntry, String> {
    List<PartnerLedgerEntry> findAllByOrderByOccurredAtDesc();
    List<PartnerLedgerEntry> findByPartyIdOrderByOccurredAtDesc(String partyId);
    @Query("select coalesce(sum(e.amountDelta), 0) from PartnerLedgerEntry e where e.partyId = :partyId")
    BigDecimal balance(String partyId);
}
