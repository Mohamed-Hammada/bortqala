package com.bemo.hr.whatsapp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WhatsAppOutboundLogRepository extends JpaRepository<WhatsAppOutboundLog, String> {
    List<WhatsAppOutboundLog> findByAppIdOrderByCreatedAtDesc(String appId);
    Optional<WhatsAppOutboundLog> findByAppIdAndDedupeKey(String appId, String dedupeKey);
    List<WhatsAppOutboundLog> findByAppIdAndStatusIn(String appId, List<String> statuses);

    @Query("SELECT w FROM WhatsAppOutboundLog w WHERE w.appId = ?1 AND w.status = 'FAILED' AND w.retryCount < 3")
    List<WhatsAppOutboundLog> findRetryable(String appId);
}
