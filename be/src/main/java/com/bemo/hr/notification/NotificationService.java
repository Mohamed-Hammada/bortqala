package com.bemo.hr.notification;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.notification.push.NotificationCreatedEvent;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final BusinessNotificationRepository notificationRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public List<NotificationApi.NotificationResponse> getNotificationsForUser(String username) {
        log.debug("getNotificationsForUser called with username={}", username);
        return getNotificationsForUser(username, Set.of());
    }

    public List<NotificationApi.NotificationResponse> getNotificationsForUser(String username, Set<String> roles) {
        log.debug("getNotificationsForUser called with username={}, rolesCount={}", username, roles.size());
        return notificationRepository.findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(username).stream()
                .map(n -> toResponse(n, roles)).sorted(Comparator.comparingInt(NotificationApi.NotificationResponse::priorityScore).reversed().thenComparing(NotificationApi.NotificationResponse::createdAt, Comparator.reverseOrder())).toList();
    }

    public NotificationApi.UnreadCountResponse getUnreadCount(String username) {
        log.debug("getUnreadCount called with username={}", username);
        long count = notificationRepository.countByRecipientUsernameIgnoreCaseAndIsReadFalse(username);
        return new NotificationApi.UnreadCountResponse(count);
    }

    @Transactional
    public NotificationApi.NotificationResponse sendNotification(NotificationApi.SendNotificationPayload payload, String senderUsername) {
        log.debug("sendNotification called with recipient={}, type={}, sender={}", payload.recipientUsername(), payload.notificationType(), senderUsername);
        var notification = new BusinessNotification(
                payload.recipientUsername(), payload.titleAr(), payload.titleEn(),
                payload.messageAr(), payload.messageEn(), payload.notificationType(),
                payload.priority(), payload.actionLink()
        );
        notification.enrich(payload.exceptionKey(), payload.impactAr(), payload.impactEn(), payload.reasonAr(), payload.reasonEn(), payload.recommendationAr(), payload.recommendationEn(), payload.impactAmount(), payload.impactCurrency(), payload.actionLabelKey(), payload.roleTargets());
        notification = notificationRepository.save(notification);
        log.info("Notification sent successfully with id={}, recipient={}", notification.getId(), payload.recipientUsername());
        auditService.record("SEND_NOTIFICATION", "NOTIFICATION", notification.getId(), senderUsername,
                "{\"recipient\":\"" + payload.recipientUsername() + "\",\"type\":\"" + payload.notificationType() + "\"}", null);
        String tenantAppId = com.bemo.hr.shared.security.TenantContext.currentOrSystem();
        eventPublisher.publishEvent(NotificationCreatedEvent.from(notification, tenantAppId));
        return toResponse(notification, Set.of());
    }

    @Transactional
    public NotificationApi.NotificationResponse markAsRead(String id, String username) {
        log.debug("markAsRead called with id={}, username={}", id, username);
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
        log.debug("markAllAsRead called with username={}", username);
        notificationRepository.markAllAsRead(username, java.time.Instant.now());
        log.info("All notifications marked as read for username={}", username);
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
