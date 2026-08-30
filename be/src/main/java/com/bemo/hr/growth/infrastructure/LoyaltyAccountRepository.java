package com.bemo.hr.growth.infrastructure;

import com.bemo.hr.growth.domain.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, String> {
    Optional<LoyaltyAccount> findByAppIdAndPartyId(String appId, String partyId);
    boolean existsByAppIdAndPartyId(String appId, String partyId);
}
