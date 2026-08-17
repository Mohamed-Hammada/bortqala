package com.bemo.hr.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MeIdentityIntegrationTests {
    @Autowired
    private AuthService authService;
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void meReturnsIdentityTenantRolesScopesAndSessionSafely() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        TenantContext.set(app.getId());

        var me = authService.me("admin", Instant.parse("2030-01-01T00:00:00Z"));

        assertThat(me.username()).isEqualTo("admin");
        assertThat(me.displayName()).isNotBlank();
        assertThat(me.tenant().id()).isEqualTo(app.getId());
        assertThat(me.tenant().code()).isEqualTo("TEST");
        assertThat(me.roles()).contains(RoleCode.ADMIN);
        assertThat(me.scopes()).contains("ADMIN");
        assertThat(me.active()).isTrue();
        assertThat(me.session().expiresAt()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
        assertThat(me.session().timeoutMinutes()).isGreaterThan(0);
        assertThat(me.session().timeoutEnabled()).isTrue();
    }
}
