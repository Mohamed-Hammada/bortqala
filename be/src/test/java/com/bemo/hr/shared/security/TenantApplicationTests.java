package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantApplicationTests {
    @Test
    void usesEightHoursByDefaultAndAcceptsAnAdministratorOverride() {
        var application = new TenantApplication("DEMO", "Demo");

        assertThat(application.getSessionTimeoutMinutes()).isEqualTo(480);
        assertThat(application.getMinPasswordLength()).isEqualTo(8);
        assertThat(application.getMaxPasswordLength()).isEqualTo(128);
        assertThat(application.isRequireUppercase()).isFalse();
        assertThat(application.isRequireLowercase()).isFalse();
        assertThat(application.isRequireNumbers()).isFalse();
        assertThat(application.isRequireSpecialChars()).isFalse();
        assertThat(application.isDisallowSpaces()).isFalse();
        assertThat(application.getPasswordExpiryDays()).isEqualTo(0);
        assertThat(application.getPasswordHistoryCount()).isEqualTo(0);

        application.updateSettings(60, true, false, 10);
        assertThat(application.getSessionTimeoutMinutes()).isEqualTo(60);
        assertThat(application.isSessionTimeoutEnabled()).isTrue();
        assertThat(application.isShowReportPresets()).isFalse();
        assertThat(application.getMinPasswordLength()).isEqualTo(10);
    }

    @Test
    void passwordPolicyUpdatesAllFields() {
        var app = new TenantApplication("DEMO", "Demo");

        app.updatePasswordPolicy(12, true, true, true, true, true, 64, 90, 5);

        assertThat(app.getMinPasswordLength()).isEqualTo(12);
        assertThat(app.isRequireUppercase()).isTrue();
        assertThat(app.isRequireLowercase()).isTrue();
        assertThat(app.isRequireNumbers()).isTrue();
        assertThat(app.isRequireSpecialChars()).isTrue();
        assertThat(app.isDisallowSpaces()).isTrue();
        assertThat(app.getMaxPasswordLength()).isEqualTo(64);
        assertThat(app.getPasswordExpiryDays()).isEqualTo(90);
        assertThat(app.getPasswordHistoryCount()).isEqualTo(5);
    }

    @Test
    void passwordPolicyClampsInvalidValues() {
        var app = new TenantApplication("DEMO", "Demo");

        app.updatePasswordPolicy(-5, false, false, false, false, false, 0, -10, -1);

        assertThat(app.getMinPasswordLength()).isEqualTo(8);
        assertThat(app.getMaxPasswordLength()).isEqualTo(128);
        assertThat(app.getPasswordExpiryDays()).isEqualTo(0);
        assertThat(app.getPasswordHistoryCount()).isEqualTo(0);
    }
}
