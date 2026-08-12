package com.bemo.hr.notification.push;

import com.bemo.hr.notification.BusinessNotification;

public record NotificationCreatedEvent(
        String appId,
        String id,
        String recipientUsername,
        String titleAr,
        String titleEn,
        String messageAr,
        String messageEn,
        String notificationType,
        String priority,
        String actionLink) {

    public static NotificationCreatedEvent from(BusinessNotification notification, String appId) {
        return new NotificationCreatedEvent(
                appId, notification.getId(), notification.getRecipientUsername(), notification.getTitleAr(),
                notification.getTitleEn(), notification.getMessageAr(), notification.getMessageEn(),
                notification.getNotificationType(), notification.getPriority(), notification.getActionLink());
    }
}
