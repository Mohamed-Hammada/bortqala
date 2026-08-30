package com.bemo.hr.trade.export.domain;

public enum ExportShipmentStatus {
    PREPARING,
    BOOKED,
    SHIPPED,
    SETTLED;

    public ExportShipmentStatus next() {
        return switch (this) {
            case PREPARING -> BOOKED;
            case BOOKED -> SHIPPED;
            case SHIPPED -> SETTLED;
            case SETTLED -> throw new IllegalStateException("SETTLED is terminal");
        };
    }

    public boolean canTransitionTo(ExportShipmentStatus target) {
        return this.next() == target;
    }
}
