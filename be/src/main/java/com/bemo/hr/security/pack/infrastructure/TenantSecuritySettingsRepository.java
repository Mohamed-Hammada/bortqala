package com.bemo.hr.security.pack.infrastructure;

import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSecuritySettingsRepository extends JpaRepository<TenantSecuritySettings, String> {
    Optional<TenantSecuritySettings> findByAppId(String appId);
}
