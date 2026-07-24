package com.bemo.hr.reporting.application;

import com.bemo.hr.shared.security.ExcelTableStyle;

public record ExcelExportOptions(String locale, ExcelTableStyle tableStyle) {
    public ExcelExportOptions {
        locale = locale != null && locale.equalsIgnoreCase("en-US") ? "en-US" : "ar-EG";
        tableStyle = tableStyle == null ? ExcelTableStyle.GOLD : tableStyle;
    }

    public boolean rightToLeft() {
        return locale.startsWith("ar");
    }
}
