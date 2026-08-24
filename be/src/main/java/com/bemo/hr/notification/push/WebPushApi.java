package com.bemo.hr.notification.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
            @Size(max = 10) String platform,
            @Size(max = 2000) String endpoint,
            Long expirationTime,
            @Valid SubscriptionKeys keys,
            @Size(max = 4096) String fcmToken,
            @Size(max = 10) String locale,
            boolean pushApprovals,
            boolean pushPayroll) {

        /** WEB subscriptions keep the VAPID contract; ANDROID ones carry an FCM token instead. */
        @AssertTrue(message = "WEB subscriptions require an https endpoint")
        public boolean hasValidWebEndpoint() {
            return isAndroid() || (endpoint != null && endpoint.matches("^https://.+"));
        }

        @AssertTrue(message = "platform ANDROID requires an fcmToken")
        public boolean isAndroidTokenPresent() {
            return !isAndroid() || (fcmToken != null && !fcmToken.isBlank());
        }

        public boolean isAndroid() {
            return "ANDROID".equalsIgnoreCase(platform);
        }
    }

    public record UnsubscribePayload(
            @NotBlank @Size(max = 2000) @Pattern(regexp = "^https://.+") String endpoint) {
    }

    public record SubscriptionStatus(boolean subscribed) {
    }
}
