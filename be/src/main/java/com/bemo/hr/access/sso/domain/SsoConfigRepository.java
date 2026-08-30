package com.bemo.hr.access.sso.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SsoConfigRepository extends JpaRepository<SsoConfig, String> {
    List<SsoConfig> findByAppIdAndActiveTrue(String appId);
    Optional<SsoConfig> findByAppIdAndProviderAndActiveTrue(String appId, String provider);
}
