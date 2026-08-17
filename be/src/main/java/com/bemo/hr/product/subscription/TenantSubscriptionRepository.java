package com.bemo.hr.product.subscription;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TenantSubscription s")
    Optional<TenantSubscription> findCurrentForUpdate();

    Optional<TenantSubscription> findFirstBy();
}
