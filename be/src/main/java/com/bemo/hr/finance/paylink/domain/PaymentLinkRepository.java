package com.bemo.hr.finance.paylink.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, String> {
    Optional<PaymentLink> findByToken(String token);
    List<PaymentLink> findByAppIdOrderByCreatedAtDesc(String appId);
    List<PaymentLink> findByAppIdAndRefId(String appId, String refId);

    @Query("SELECT pl FROM PaymentLink pl WHERE pl.appId = ?1 AND pl.status = 'PENDING' AND pl.expiresAt IS NOT NULL AND pl.expiresAt < CURRENT_TIMESTAMP")
    List<PaymentLink> findExpiredPending(String appId);
}
