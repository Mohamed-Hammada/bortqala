package com.bemo.hr.notification;

import jakarta.validation.constraints.NotBlank;

public final class NotificationApi {
    private NotificationApi() { }

    public record NotificationResponse(
            String id,
            String recipientUsername,
            String titleAr,
            String titleEn,
            String messageAr,
            String messageEn,
            String notificationType,
            String priority,
            String actionLink,
            boolean isRead,
            Long readAt,
            long createdAt
    ) { }

    public record UnreadCountResponse(
            long unreadCount
    ) { }

    public record SendNotificationPayload(
            @NotBlank String recipientUsername,
            @NotBlank String titleAr,
            @NotBlank String titleEn,
            @NotBlank String messageAr,
            @NotBlank String messageEn,
            String notificationType,
            String priority,
            String actionLink
    ) { }
}
