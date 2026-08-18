package com.bemo.hr.product.pack;

public interface IndustryKpiProvider {
    String key();

    KpiResult calculate();

    record KpiResult(
            String key,
            String labelKey,
            double value,
            String unit,
            String status
    ) {}
}
