package com.bemo.hr.product.subscription;
import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;import java.util.Optional;
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription,String>{@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select s from TenantSubscription s")Optional<TenantSubscription> findCurrentForUpdate();Optional<TenantSubscription> findFirstBy();}
