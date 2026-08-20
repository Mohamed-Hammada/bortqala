package com.bemo.hr.analytics.domain;

public record KpiDefinition(
        String key,
        String nameEn,
        String nameAr,
        KpiCategory category,
        KpiGrain grain,
        KpiUnit unit,
        String formulaEn,
        String formulaAr,
        String sourceModule,
        String requiredPermission
) {}
