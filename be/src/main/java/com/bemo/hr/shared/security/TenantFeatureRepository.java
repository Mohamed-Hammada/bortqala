package com.bemo.hr.shared.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantFeatureRepository extends JpaRepository<TenantFeature, TenantFeatureId> {
    List<TenantFeature> findByAppId(String appId);
}
