package com.bemo.hr.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {
    List<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(String endpointId);
    List<WebhookDelivery> findByEndpointIdAndStatusOrderByCreatedAtDesc(String endpointId, String status);
    long countByEndpointIdAndStatus(String endpointId, String status);
}
