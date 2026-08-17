package com.bemo.hr.notification.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class WebPushApi {
    private WebPushApi() {
    }

    public record ConfigResponse(boolean enabled, String publicKey) {
    }

    public record SubscriptionKeys(
            @NotBlank @Size(max = 512) String p256dh,
            @NotBlank @Size(max = 256) String auth) {
    }

    public record SubscriptionPayload(
            @NotBlank @Size(max = 2000) @Pattern(regexp = "^https://.+") String endpoint,
            Long expirationTime,
            @Valid @NotNull SubscriptionKeys keys,
            @Size(max = 10) String locale,
            boolean pushApprovals,
            boolean pushPayroll) {
    }

    public record UnsubscribePayload(
            @NotBlank @Size(max = 2000) @Pattern(regexp = "^https://.+") String endpoint) {
    }

    public record SubscriptionStatus(boolean subscribed) {
    }
}
