package com.bemo.hr.growth.infrastructure;

import com.bemo.hr.growth.domain.LoyaltyLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyLedgerEntryRepository extends JpaRepository<LoyaltyLedgerEntry, String> {
    List<LoyaltyLedgerEntry> findByLoyaltyAccountIdOrderByCreatedAtDesc(String loyaltyAccountId);
    List<LoyaltyLedgerEntry> findByAppIdAndPartyIdOrderByCreatedAtDesc(String appId, String partyId);
}
