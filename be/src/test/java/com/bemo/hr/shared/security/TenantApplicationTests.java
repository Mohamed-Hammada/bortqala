package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantApplicationTests {
    @Test
    void usesEightHoursByDefaultAndAcceptsAnAdministratorOverride() {
        var application = new TenantApplication("DEMO", "Demo");

        assertThat(application.getSessionTimeoutMinutes()).isEqualTo(480);
        assertThat(application.getMinPasswordLength()).isEqualTo(8);

        application.updateSettings(60, true, false, 10);
        assertThat(application.getSessionTimeoutMinutes()).isEqualTo(60);
        assertThat(application.isSessionTimeoutEnabled()).isTrue();
        assertThat(application.isShowReportPresets()).isFalse();
        assertThat(application.getMinPasswordLength()).isEqualTo(10);
    }
}
