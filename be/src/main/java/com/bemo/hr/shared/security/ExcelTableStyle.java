package com.bemo.hr.shared.security;

public enum ExcelTableStyle {
    GOLD("TableStyleMedium12"),
    BLUE("TableStyleMedium2"),
    GREEN("TableStyleMedium4"),
    GRAY("TableStyleMedium3");

    private final String poiStyleName;

    ExcelTableStyle(String poiStyleName) {
        this.poiStyleName = poiStyleName;
    }

    public String poiStyleName() {
        return poiStyleName;
    }
}
