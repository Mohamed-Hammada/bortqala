package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.LinkedHashSet;
import java.util.List;

class UserPreferenceTests {
    @Test
    void keepsSupportedLocaleInCanonicalForm() {
        var preference = new UserPreference("user-1");

        preference.update(ThemePreference.DARK, TableDensity.COMPACT, "en-us", ExcelTableStyle.BLUE);
        assertThat(preference.getLocale()).isEqualTo("en-US");
        assertThat(preference.getExcelTableStyle()).isEqualTo(ExcelTableStyle.BLUE);

        preference.update(ThemePreference.LIGHT, TableDensity.COMFORTABLE, "ar-eg", ExcelTableStyle.GOLD);
        assertThat(preference.getLocale()).isEqualTo("ar-EG");
    }

    @Test
    void keepsNavigationPreferencesOrderedAndWithinTheConfiguredLimit() {
        var preference = new UserPreference("user-1");

        preference.updateNavigation(false, true, 2,
                new LinkedHashSet<>(java.util.List.of("employees", "bad id", "reports")),
                new LinkedHashSet<>(java.util.List.of("dashboard", "reports", "employees")));

        assertThat(preference.isShowFavorites()).isFalse();
        assertThat(preference.isShowRecentlyUsed()).isTrue();
        assertThat(preference.getMaxRecentlyUsed()).isEqualTo(2);
        assertThat(preference.favoriteMenuIds()).containsExactly("employees", "reports");
        assertThat(preference.recentMenuIds()).containsExactly("dashboard", "reports");
    }

    @Test
    void persistsAValidatedDashboardOrderAndIndependentMotionPreference() {
        var preference = new UserPreference("user-1");

        preference.updateDashboard(List.of("insights", "summary", "insights", "unknown"), false, true);

        assertThat(preference.dashboardWidgetIds()).containsExactly("insights", "summary");
        assertThat(preference.isDashboardAnimationsEnabled()).isFalse();
    }

    @Test
    void changesMotionButKeepsLayoutWhenCustomizationIsNotAllowed() {
        var preference = new UserPreference("user-1");
        preference.updateDashboard(List.of("summary", "insights"), true, true);

        preference.updateDashboard(List.of("imports"), false, false);

        assertThat(preference.dashboardWidgetIds()).containsExactly("summary", "insights");
        assertThat(preference.isDashboardAnimationsEnabled()).isFalse();
    }
}
