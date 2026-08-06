package com.bemo.hr.notification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationApi.NotificationResponse> getMyNotifications(Authentication authentication) {
        return notificationService.getNotificationsForUser(authentication.getName());
    }

    @GetMapping("/unread-count")
    public NotificationApi.UnreadCountResponse getUnreadCount(Authentication authentication) {
        return notificationService.getUnreadCount(authentication.getName());
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public NotificationApi.NotificationResponse sendNotification(
            @Valid @RequestBody NotificationApi.SendNotificationPayload payload,
            Authentication authentication) {
        return notificationService.sendNotification(payload, authentication.getName());
    }

    @PostMapping("/{id}/read")
    public NotificationApi.NotificationResponse markAsRead(@PathVariable String id, Authentication authentication) {
        return notificationService.markAsRead(id, authentication.getName());
    }

    @PostMapping("/read-all")
    public void markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
    }
}
