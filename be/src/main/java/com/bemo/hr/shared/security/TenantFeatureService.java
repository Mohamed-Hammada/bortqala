package com.bemo.hr.shared.security;

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
        // Defaults mapping from the production guide
        DEFAULTS.put("employeeAttendance.enabled", true);
        DEFAULTS.put("biometric.fileImport.enabled", true);
        DEFAULTS.put("biometric.liveSync.enabled", false);
        DEFAULTS.put("workforce.enabled", true);
        DEFAULTS.put("workforce.contractorAccounts.enabled", false);
        DEFAULTS.put("payroll.enabled", false);
        DEFAULTS.put("procurement.enabled", true);
        DEFAULTS.put("inventory.advanced.enabled", false);
        DEFAULTS.put("sales.enabled", false);
        DEFAULTS.put("manufacturing.enabled", false);
        DEFAULTS.put("quality.enabled", false);
        DEFAULTS.put("finance.enabled", false);
        DEFAULTS.put("exports.enabled", true);
        DEFAULTS.put("notifications.enabled", false);
        DEFAULTS.put("navigation.favorites.enabled", true);
        DEFAULTS.put("navigation.recents.enabled", true);
    }

    public TenantFeatureService(TenantFeatureRepository repository) {
        this.repository = repository;
    }

    public boolean isEnabled(String appId, String featureKey) {
        return repository.findById(new TenantFeatureId(appId, featureKey))
                .map(TenantFeature::isEnabled)
                .orElseGet(() -> DEFAULTS.getOrDefault(featureKey, false));
    }

    public Set<String> getAllEnabled(String appId) {
        var dbFeatures = repository.findByAppId(appId).stream()
                .collect(Collectors.toMap(TenantFeature::getFeatureKey, TenantFeature::isEnabled));

        return DEFAULTS.entrySet().stream()
                .filter(entry -> dbFeatures.getOrDefault(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
