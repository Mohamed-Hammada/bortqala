package com.bemo.hr.trade.pos.domain;

public enum ThermalPaperWidth {
    MM_58(32),
    MM_80(48);

    private final int columns;

    ThermalPaperWidth(int columns) {
        this.columns = columns;
    }

    public int getColumns() {
        return columns;
    }
}
