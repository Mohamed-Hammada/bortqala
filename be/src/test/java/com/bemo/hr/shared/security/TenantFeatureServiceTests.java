package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantFeatureServiceTests {

    @Mock
    private TenantFeatureRepository repository;

    private TenantFeatureService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() { service = new TenantFeatureService(repository, new EntitlementCatalog()); }

    @Test
    void defaultEnabledFeaturesAreReturnedWhenNoDbRows() {
        when(repository.findByAppId("app-1")).thenReturn(List.of());

        Set<String> enabled = service.getAllEnabled("app-1");

        assertThat(enabled)
                .contains("employeeAttendance.enabled", "biometric.fileImport.enabled",
                        "workforce.enabled", "procurement.enabled", "exports.enabled",
                        "payroll.enabled", "sales.enabled", "manufacturing.enabled",
                        "finance.enabled", "quality.enabled",
                        "navigation.favorites.enabled", "navigation.recents.enabled")
                .doesNotContain("biometric.liveSync.enabled", "notifications.enabled");
    }

    @Test
    void dbEnabledCustomFeatureNotInDefaultsIsMerged() {
        when(repository.findByAppId("app-1"))
                .thenReturn(List.of(new TenantFeature("app-1", "custom.module.enabled", true, null, "tester")));

        Set<String> enabled = service.getAllEnabled("app-1");

        assertThat(enabled).contains("custom.module.enabled");
    }

    @Test
    void dbOverrideDisablesDefaultEnabledFeature() {
        when(repository.findByAppId("app-1"))
                .thenReturn(List.of(new TenantFeature("app-1", "exports.enabled", false, null, "tester")));

        assertThat(service.getAllEnabled("app-1")).doesNotContain("exports.enabled");
    }

    @Test
    void dbOverrideEnablesDefaultDisabledFeature() {
        when(repository.findByAppId("app-1"))
                .thenReturn(List.of(new TenantFeature("app-1", "payroll.enabled", true, null, "tester")));

        assertThat(service.getAllEnabled("app-1")).contains("payroll.enabled");
    }

    @Test
    void disabledCustomFeatureIsExcludedEvenWhenEnabledByDefault() {
        when(repository.findByAppId("app-1"))
                .thenReturn(List.of(new TenantFeature("app-1", "workforce.enabled", false, null, "tester")));

        assertThat(service.getAllEnabled("app-1")).doesNotContain("workforce.enabled");
    }

    @Test
    void isEnabledFallsBackToDefaultsAndUnknownKeysAreDisabled() {
        when(repository.findById(new TenantFeatureId("app-1", "payroll.enabled"))).thenReturn(Optional.empty());
        when(repository.findById(new TenantFeatureId("app-1", "exports.enabled"))).thenReturn(Optional.empty());
        when(repository.findById(new TenantFeatureId("app-1", "notifications.enabled"))).thenReturn(Optional.empty());
        when(repository.findById(new TenantFeatureId("app-1", "unknown.key"))).thenReturn(Optional.empty());

        assertThat(service.isEnabled("app-1", "payroll.enabled")).isTrue();
        assertThat(service.isEnabled("app-1", "exports.enabled")).isTrue();
        assertThat(service.isEnabled("app-1", "notifications.enabled")).isFalse();
        assertThat(service.isEnabled("app-1", "unknown.key")).isFalse();
    }

    @Test
    void isEnabledUsesDbValueWhenPresent() {
        when(repository.findById(new TenantFeatureId("app-1", "payroll.enabled")))
                .thenReturn(Optional.of(new TenantFeature("app-1", "payroll.enabled", true, null, "tester")));
        when(repository.findById(new TenantFeatureId("app-1", "exports.enabled")))
                .thenReturn(Optional.of(new TenantFeature("app-1", "exports.enabled", false, null, "tester")));

        assertThat(service.isEnabled("app-1", "payroll.enabled")).isTrue();
        assertThat(service.isEnabled("app-1", "exports.enabled")).isFalse();
    }
}
