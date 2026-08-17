package com.bemo.hr.finance.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public final class ExchangeRateHintApi {
    private ExchangeRateHintApi() {
    }

    public record SettingsResponse(
            String provider,
            boolean enabled,
            int refreshIntervalHours,
            Long lastAttemptAt,
            Long lastSuccessAt,
            Long nextRefreshAt,
            String lastErrorCode
    ) {
    }

    public record SettingsRequest(
            boolean enabled,
            @Min(1) @Max(168) int refreshIntervalHours
    ) {
    }

    public record RefreshResponse(
            boolean success,
            int refreshedCount,
            int unsupportedCount,
            String baseCurrency,
            Long fetchedAt,
            String errorCode
    ) {
    }
}
