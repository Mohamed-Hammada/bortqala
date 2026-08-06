package com.bemo.hr.notification;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final BusinessNotificationRepository notificationRepository;
    private final AuditService auditService;

    public List<NotificationApi.NotificationResponse> getNotificationsForUser(String username) {
        return notificationRepository.findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(username).stream()
                .map(this::toResponse).toList();
    }

    public NotificationApi.UnreadCountResponse getUnreadCount(String username) {
        long count = notificationRepository.countByRecipientUsernameIgnoreCaseAndIsReadFalse(username);
        return new NotificationApi.UnreadCountResponse(count);
    }

    @Transactional
    public NotificationApi.NotificationResponse sendNotification(NotificationApi.SendNotificationPayload payload, String senderUsername) {
        var notification = new BusinessNotification(
                payload.recipientUsername(), payload.titleAr(), payload.titleEn(),
                payload.messageAr(), payload.messageEn(), payload.notificationType(),
                payload.priority(), payload.actionLink()
        );
        notification = notificationRepository.save(notification);
        auditService.record("SEND_NOTIFICATION", "NOTIFICATION", notification.getId(), senderUsername,
                "{\"recipient\":\"" + payload.recipientUsername() + "\",\"type\":\"" + payload.notificationType() + "\"}", null);
        return toResponse(notification);
    }

    @Transactional
    public NotificationApi.NotificationResponse markAsRead(String id, String username) {
        var notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id, "NOTIFICATION_NOT_FOUND"));
        if (!notification.getRecipientUsername().equalsIgnoreCase(username)) {
            throw new NotFoundException("Notification not found for user: " + id, "NOTIFICATION_NOT_FOUND");
        }
        notification.markRead();
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsRead(username, java.time.Instant.now());
    }

    private NotificationApi.NotificationResponse toResponse(BusinessNotification n) {
        return new NotificationApi.NotificationResponse(
                n.getId(), n.getRecipientUsername(), n.getTitleAr(), n.getTitleEn(),
                n.getMessageAr(), n.getMessageEn(), n.getNotificationType(), n.getPriority(),
                n.getActionLink(), n.isRead(), n.getReadAt() != null ? n.getReadAt().toEpochMilli() : null,
                n.getCreatedAt().toEpochMilli()
        );
    }
}
