package com.bemo.hr.growth.infrastructure;

import com.bemo.hr.growth.domain.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, String> {
    Optional<Referral> findByAppIdAndReferredPartyId(String appId, String referredPartyId);
    Optional<Referral> findByAppIdAndId(String appId, String id);
    List<Referral> findByAppIdAndReferrerPartyIdOrderByCreatedAtDesc(String appId, String referrerPartyId);
    long countByAppIdAndReferrerPartyIdAndStatusIn(String appId, String referrerPartyId, List<String> statuses);
    boolean existsByAppIdAndReferredPartyId(String appId, String referredPartyId);
}
