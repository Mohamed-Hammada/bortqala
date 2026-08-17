package com.bemo.hr.notification.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, String> {
    Optional<WebPushSubscription> findByEndpointHash(String endpointHash);

    List<WebPushSubscription> findByUsernameIgnoreCaseAndEnabledTrue(String username);
}
