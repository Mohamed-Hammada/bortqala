package com.bemo.hr.shared.security;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EntitlementCatalog {
    private final List<Feature> features = List.of(
            f("employeeAttendance.enabled", "HR", Set.of(), "/api/v1/reports", "/api/v1/attendance"),
            f("biometric.fileImport.enabled", "HR", Set.of("employeeAttendance.enabled"), "/api/v1/imports"),
            f("biometric.liveSync.enabled", "HR", Set.of("employeeAttendance.enabled"), "/api/v1/attendance/device-integrations"),
            f("workforce.enabled", "WORKFORCE", Set.of(), "/api/v1/workforce"),
            f("workforce.attendance.enabled", "WORKFORCE", Set.of("workforce.enabled"), "/api/v1/workforce/attendance"),
            f("workforce.dashboard.enabled", "WORKFORCE", Set.of("workforce.enabled"), "/api/v1/workforce/dashboard"),
            f("workforce.contractorAccounts.enabled", "WORKFORCE", Set.of("workforce.enabled"), "/api/v1/workforce/contractors", "/api/v1/workforce/settlements"),
            f("payroll.enabled", "PAYROLL", Set.of("employeeAttendance.enabled"), "/api/v1/payroll"),
            f("procurement.enabled", "PROCUREMENT", Set.of(), "/api/v1/trade/procurement"),
            f("purchasing.enabled", "PROCUREMENT", Set.of()),
            f("inventory.advanced.enabled", "INVENTORY", Set.of("procurement.enabled"), "/api/v1/operations/valuation"),
            f("sales.enabled", "SALES", Set.of(), "/api/v1/trade/sales"),
            f("agri.enabled", "AGRI", Set.of(), "/api/v1/trade/export-shipments"),
            f("manufacturing.enabled", "MANUFACTURING", Set.of("inventory.advanced.enabled"), "/api/v1/manufacturing"),
            f("quality.enabled", "QUALITY", Set.of(), "/api/v1/quality"),
            f("finance.enabled", "FINANCE", Set.of(), "/api/v1/finance", "/api/v1/fiscal-periods"),
            f("exports.enabled", "PLATFORM", Set.of(), "/api/v1/exports"),
            f("notifications.enabled", "PLATFORM", Set.of(), "/api/v1/notifications"),
            f("navigation.favorites.enabled", "PLATFORM", Set.of()),
            f("navigation.recents.enabled", "PLATFORM", Set.of()));
    private final Map<String, Feature> byKey = features.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Feature::key, java.util.function.Function.identity()));

    private static Feature f(String key, String module, Set<String> dependencies, String... prefixes) {
        return f(true, key, module, dependencies, prefixes);
    }

    private static Feature f(boolean enabled, String key, String module, Set<String> dependencies, String... prefixes) {
        return new Feature(key, module, enabled, dependencies, List.of(prefixes));
    }

    public List<Module> modules() {
        return features.stream().collect(java.util.stream.Collectors.groupingBy(Feature::moduleKey, LinkedHashMap::new, java.util.stream.Collectors.toList())).entrySet().stream().map(e -> new Module(e.getKey(), List.copyOf(e.getValue()))).toList();
    }

    public Optional<Feature> feature(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public boolean defaultEnabled(String key) {
        return feature(key).map(Feature::defaultEnabled).orElse(false);
    }

    public Map<String, Boolean> defaults() {
        return features.stream().collect(java.util.stream.Collectors.toMap(Feature::key, Feature::defaultEnabled));
    }

    public Set<String> dependents(String key) {
        return features.stream().filter(f -> f.dependencies().contains(key)).map(Feature::key).collect(java.util.stream.Collectors.toSet());
    }

    public Optional<String> requiredFeature(String uri) {
        return features.stream().flatMap(f -> f.apiPrefixes().stream().map(p -> Map.entry(p, f.key()))).filter(e -> uri.startsWith(e.getKey())).max(Comparator.comparingInt(e -> e.getKey().length())).map(Map.Entry::getValue);
    }

    public record Feature(String key, String moduleKey, boolean defaultEnabled, Set<String> dependencies,
                          List<String> apiPrefixes) {
    }

    public record Module(String key, List<Feature> features) {
    }
}
