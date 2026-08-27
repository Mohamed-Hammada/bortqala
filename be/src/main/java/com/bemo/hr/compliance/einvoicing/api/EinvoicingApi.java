package com.bemo.hr.compliance.einvoicing.api;

import com.bemo.hr.compliance.einvoicing.domain.EinvoicingEnvironment;
import com.bemo.hr.compliance.einvoicing.domain.EinvoicingProviderType;
import jakarta.validation.constraints.NotNull;

public final class EinvoicingApi {

    private EinvoicingApi() {
    }

    public record SettingsResponse(
            String id,
            EinvoicingProviderType provider,
            EinvoicingEnvironment environment,
            long createdAt,
            long updatedAt
    ) {
    }

    public record SaveSettingsRequest(
            @NotNull EinvoicingProviderType provider,
            @NotNull EinvoicingEnvironment environment
    ) {
    }

    public record ProviderInfo(
            EinvoicingProviderType type,
            String labelKey,
            boolean implemented
    ) {
    }
}
