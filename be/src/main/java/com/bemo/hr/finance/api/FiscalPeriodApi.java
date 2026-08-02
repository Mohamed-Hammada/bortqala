package com.bemo.hr.finance.api;

import jakarta.validation.constraints.NotNull;

public class FiscalPeriodApi {

    public record FiscalPeriodResponse(
            String id,
            int fiscalYear,
            int periodNumber,
            String periodName,
            long startDate,
            long endDate,
            String status,
            String closedBy,
            Long closedAt,
            long version,
            long createdAt,
            long updatedAt
    ) {}

    public record CreatePeriodPayload(
            int fiscalYear,
            int periodNumber,
            @NotNull String periodName,
            long startDate,
            long endDate
    ) {}

    public record UpdateStatusPayload(
            @NotNull String status,
            Long expectedVersion
    ) {}
}
