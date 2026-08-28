package com.bemo.hr.access.sso.application;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface SsoApi {

    record CreateConfigRequest(
            @NotBlank String provider,
            @NotBlank String clientId,
            @NotBlank String clientSecret,
            String issuer,
            String discoveryUrl,
            boolean autoProvision,
            String defaultRole
    ) {}

    record UpdateConfigRequest(
            String clientId,
            String clientSecret,
            String issuer,
            String discoveryUrl,
            Boolean autoProvision,
            String defaultRole,
            Boolean active
    ) {}

    record ConfigResponse(
            String id,
            String provider,
            String clientId,
            String issuer,
            String discoveryUrl,
            boolean autoProvision,
            String defaultRole,
            boolean active,
            Long version
    ) {
        public static ConfigResponse from(com.bemo.hr.access.sso.domain.SsoConfig c) {
            return new ConfigResponse(
                    c.getId(), c.getProvider().name(), c.getClientId(), c.getIssuer(),
                    c.getDiscoveryUrl(), c.isAutoProvision(), c.getDefaultRole(),
                    c.isActive(), c.getVersion()
            );
        }
    }

    record StartResponse(String authorizationUrl, String stateToken) {}

    record CallbackResult(
            String userId,
            boolean newlyProvisioned,
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            String username,
            List<String> roles
    ) {}

    record IdentityResponse(String id, String provider, String email, String displayName) {}

    record ProbeResponse(boolean hasGoogle, boolean hasMicrosoft) {}
}
