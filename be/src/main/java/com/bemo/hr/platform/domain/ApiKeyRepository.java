package com.bemo.hr.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByAppIdAndKeyHash(String appId, String keyHash);
    List<ApiKey> findByAppIdOrderByCreatedAtDesc(String appId);
    boolean existsByAppIdAndKeyHash(String appId, String keyHash);
}
