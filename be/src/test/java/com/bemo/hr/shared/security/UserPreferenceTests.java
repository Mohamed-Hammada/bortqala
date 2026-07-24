package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
