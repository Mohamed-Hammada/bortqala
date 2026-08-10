package com.bemo.hr.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TenantFeatureService {

    private final TenantFeatureRepository repository;
    private final EntitlementCatalog catalog;

    public TenantFeatureService(TenantFeatureRepository repository,EntitlementCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public boolean isEnabled(String appId, String featureKey) {
        // SUPER_ADMIN must be able to reach every implemented module even when
        // a tenant-level feature row is disabled. Normal roles still respect
        // the tenant feature configuration.
        if (currentUserIsSuperAdmin()) {
            return true;
        }
        return isEnabledForTenant(appId, featureKey);
    }

    public boolean isEnabledForTenant(String appId, String featureKey) {
        return repository.findById(new TenantFeatureId(appId, featureKey))
                .map(TenantFeature::isEnabled)
                .orElseGet(() -> catalog.defaultEnabled(featureKey));
    }

    public Set<String> getAllEnabled(String appId) {
        Map<String, Boolean> effective = new java.util.HashMap<>(catalog.defaults());
        repository.findByAppId(appId).forEach(feature -> effective.put(feature.getFeatureKey(), feature.isEnabled()));

        if (currentUserIsSuperAdmin()) {
            return Set.copyOf(effective.keySet());
        }

        return effective.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private boolean currentUserIsSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }
}
