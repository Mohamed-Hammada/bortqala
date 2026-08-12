package com.bemo.hr.notification.push;

import com.bemo.hr.notification.NotificationApi;
import com.bemo.hr.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/push")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WebPushController {
    private final WebPushService webPushService;
    private final NotificationService notificationService;

    @GetMapping("/config")
    public WebPushApi.ConfigResponse config() {
        return webPushService.config();
    }

    @GetMapping("/status")
    public WebPushApi.SubscriptionStatus status(Authentication authentication) {
        return webPushService.status(authentication.getName());
    }

    @PostMapping("/subscriptions")
    public WebPushApi.SubscriptionStatus subscribe(
            @Valid @RequestBody WebPushApi.SubscriptionPayload payload,
            Authentication authentication) {
        return webPushService.register(authentication.getName(), payload);
    }

    @PostMapping("/subscriptions/unsubscribe")
    public void unsubscribe(
            @Valid @RequestBody WebPushApi.UnsubscribePayload payload,
            Authentication authentication) {
        webPushService.unsubscribe(authentication.getName(), payload);
    }

    @PostMapping("/subscriptions/unsubscribe-all")
    public void unsubscribeAll(Authentication authentication) {
        webPushService.unsubscribeAll(authentication.getName());
    }

    @PostMapping("/test")
    public NotificationApi.NotificationResponse test(Authentication authentication) {
        var payload = new NotificationApi.SendNotificationPayload(
                authentication.getName(),
                "اختبار إشعارات BEMO",
                "BEMO push notification test",
                "إذا ظهر هذا الإشعار على جهازك، فإن Web Push يعمل بنجاح.",
                "If this notification appeared on your device, Web Push is working.",
                "WEB_PUSH_TEST",
                "INFO",
                "/settings");
        return notificationService.sendNotification(payload, authentication.getName());
    }
}
