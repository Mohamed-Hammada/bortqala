package com.bemo.hr.notification;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.notification.push.NotificationCreatedEvent;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final BusinessNotificationRepository notificationRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public List<NotificationApi.NotificationResponse> getNotificationsForUser(String username) {
        return getNotificationsForUser(username, Set.of());
    }

    public List<NotificationApi.NotificationResponse> getNotificationsForUser(String username, Set<String> roles) {
        return notificationRepository.findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(username).stream()
                .map(n -> toResponse(n, roles)).sorted(Comparator.comparingInt(NotificationApi.NotificationResponse::priorityScore).reversed().thenComparing(NotificationApi.NotificationResponse::createdAt, Comparator.reverseOrder())).toList();
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
        notification.enrich(payload.exceptionKey(), payload.impactAr(), payload.impactEn(), payload.reasonAr(), payload.reasonEn(), payload.recommendationAr(), payload.recommendationEn(), payload.impactAmount(), payload.impactCurrency(), payload.actionLabelKey(), payload.roleTargets());
        notification = notificationRepository.save(notification);
        auditService.record("SEND_NOTIFICATION", "NOTIFICATION", notification.getId(), senderUsername,
                "{\"recipient\":\"" + payload.recipientUsername() + "\",\"type\":\"" + payload.notificationType() + "\"}", null);
        String tenantAppId = com.bemo.hr.shared.security.TenantContext.currentOrSystem();
        eventPublisher.publishEvent(NotificationCreatedEvent.from(notification, tenantAppId));
        return toResponse(notification, Set.of());
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
        return toResponse(notification, Set.of());
    }

    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsRead(username, java.time.Instant.now());
    }

    private NotificationApi.NotificationResponse toResponse(BusinessNotification n, Set<String> roles) {
        int score = switch (n.getPriority()) {
            case "CRITICAL" -> 100;
            case "HIGH" -> 75;
            case "MEDIUM" -> 50;
            default -> 25;
        };
        if (!n.isRead()) score += 10;
        if (n.targetRoles().stream().anyMatch(roles::contains)) score += 30;
        return new NotificationApi.NotificationResponse(
                n.getId(), n.getRecipientUsername(), n.getTitleAr(), n.getTitleEn(),
                n.getMessageAr(), n.getMessageEn(), n.getNotificationType(), n.getPriority(),
                n.getActionLink(), n.getExceptionKey(), n.getImpactAr(), n.getImpactEn(), n.getReasonAr(), n.getReasonEn(), n.getRecommendationAr(), n.getRecommendationEn(), n.getImpactAmount(), n.getImpactCurrency(), n.getActionLabelKey(), n.targetRoles(), score, n.isRead(), n.getReadAt() != null ? n.getReadAt().toEpochMilli() : null,
                n.getCreatedAt().toEpochMilli()
        );
    }
}
