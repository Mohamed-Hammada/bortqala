package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantApplicationTests {
    @Test
    void usesEightHoursByDefaultAndAcceptsAnAdministratorOverride() {
        var application = new TenantApplication("DEMO", "Demo");

        assertThat(application.getSessionTimeoutMinutes()).isEqualTo(480);

        application.updateSessionTimeoutMinutes(60);
        assertThat(application.getSessionTimeoutMinutes()).isEqualTo(60);
    }
}
