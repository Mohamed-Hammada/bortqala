package com.bemo.hr.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TenantFeatureService {

    private final TenantFeatureRepository repository;
    private static final Map<String, Boolean> DEFAULTS = new HashMap<>();

    static {
        // Full-ERP branch defaults. Explicit tenant rows in tenant_features still override these values.
        DEFAULTS.put("employeeAttendance.enabled", true);
        DEFAULTS.put("biometric.fileImport.enabled", true);
        DEFAULTS.put("biometric.liveSync.enabled", false);

        DEFAULTS.put("workforce.enabled", true);
        DEFAULTS.put("workforce.attendance.enabled", true);
        DEFAULTS.put("workforce.dashboard.enabled", true);
        DEFAULTS.put("workforce.contractorAccounts.enabled", true);

        DEFAULTS.put("payroll.enabled", true);
        DEFAULTS.put("procurement.enabled", true);
        DEFAULTS.put("purchasing.enabled", true);
        DEFAULTS.put("inventory.advanced.enabled", true);
        DEFAULTS.put("sales.enabled", true);
        DEFAULTS.put("manufacturing.enabled", true);
        DEFAULTS.put("quality.enabled", true);
        DEFAULTS.put("finance.enabled", true);

        DEFAULTS.put("exports.enabled", true);
        DEFAULTS.put("notifications.enabled", false);
        DEFAULTS.put("navigation.favorites.enabled", true);
        DEFAULTS.put("navigation.recents.enabled", true);
    }

    public TenantFeatureService(TenantFeatureRepository repository) {
        this.repository = repository;
    }

    public boolean isEnabled(String appId, String featureKey) {
        // SUPER_ADMIN must be able to reach every implemented module even when
        // a tenant-level feature row is disabled. Normal roles still respect
        // the tenant feature configuration.
        if (currentUserIsSuperAdmin()) {
            return true;
        }
        return repository.findById(new TenantFeatureId(appId, featureKey))
                .map(TenantFeature::isEnabled)
                .orElseGet(() -> DEFAULTS.getOrDefault(featureKey, false));
    }

    public Set<String> getAllEnabled(String appId) {
        Map<String, Boolean> effective = new HashMap<>(DEFAULTS);
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
