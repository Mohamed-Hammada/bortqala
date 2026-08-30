package com.bemo.hr.growth.infrastructure;

import com.bemo.hr.growth.domain.MemberSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, String> {
    List<MemberSubscription> findByAppIdAndPartyId(String appId, String partyId);
    Optional<MemberSubscription> findByAppIdAndPartyIdAndStatusIn(String appId, String partyId, List<String> statuses);
    List<MemberSubscription> findByAppIdAndStatusIn(String appId, List<String> statuses);
    List<MemberSubscription> findByAppIdAndStatusAndCurrentPeriodEndLessThanEqual(String appId, String status, long timestamp);
    long countByAppIdAndPartyIdAndStatus(String appId, String partyId, String status);
}
