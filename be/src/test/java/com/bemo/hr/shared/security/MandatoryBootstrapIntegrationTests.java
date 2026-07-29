package com.bemo.hr.shared.security;

import com.bemo.hr.shared.i18n.TranslationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MandatoryBootstrapIntegrationTests {
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TranslationRepository translationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productionCatalogAndConfiguredAdministratorsAreAlwaysPresent() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        var admin = appUserRepository.findByAppIdAndUsernameIgnoreCase(app.getId(), "admin").orElseThrow();
        var superAdmin = appUserRepository.findByAppIdAndUsernameIgnoreCase(app.getId(), "superadmin").orElseThrow();

        assertThat(roleRepository.existsById(RoleCode.ADMIN)).isTrue();
        assertThat(roleRepository.existsById(RoleCode.SUPER_ADMIN)).isTrue();
        assertThat(roleCodes(admin)).containsExactly(RoleCode.ADMIN);
        assertThat(roleCodes(superAdmin)).contains(RoleCode.ADMIN, RoleCode.SUPER_ADMIN);
        assertThat(translationRepository.count()).isGreaterThan(2_000);
    }

    @Test
    void testOnlyMasterLoadsFixturesIntoTheIsolatedTestTenant() {
        assertThat(count("attendance_categories")).isEqualTo(9);
        assertThat(count("companies")).isEqualTo(1);
        assertThat(count("currencies")).isEqualTo(3);
    }

    private Set<RoleCode> roleCodes(AppUser user) {
        return user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
