package com.bemo.hr.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, String> {
    List<WebhookEndpoint> findByAppIdOrderByCreatedAtDesc(String appId);
    List<WebhookEndpoint> findByAppIdAndActiveTrue(String appId);
}
