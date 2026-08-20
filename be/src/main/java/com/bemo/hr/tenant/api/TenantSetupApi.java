package com.bemo.hr.tenant.api;

import com.bemo.hr.tenant.domain.BusinessVertical;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public final class TenantSetupApi {

    private TenantSetupApi() {}

    public record ConfigureVerticalRequest(
            @NotNull(message = "vertical is required")
            BusinessVertical vertical
    ) {}

    public record TenantVerticalResponse(
            String appId,
            BusinessVertical vertical,
            Set<String> activeFeatures,
            List<String> provisionedPolicyGroups
    ) {}
}
