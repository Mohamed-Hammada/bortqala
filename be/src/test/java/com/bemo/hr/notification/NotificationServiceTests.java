package com.bemo.hr.notification;

import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock private BusinessNotificationRepository notificationRepository;
    @Mock private AuditService auditService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, auditService);
    }

    @Test
    void sendNotification_savesNotificationAndRecordsAudit() {
        var payload = new NotificationApi.SendNotificationPayload(
                "admin", "تنبيه جديد", "New Alert",
                "تفاصيل التنبيه", "Alert details",
                "APPROVAL_REQUIRED", "HIGH", "/approvals/my-tasks"
        );

        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = notificationService.sendNotification(payload, "system");

        assertThat(response.recipientUsername()).isEqualTo("admin");
        assertThat(response.notificationType()).isEqualTo("APPROVAL_REQUIRED");
        assertThat(response.isRead()).isFalse();
        verify(auditService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void markAsRead_updatesNotificationStatus() {
        var notification = new BusinessNotification(
                "admin", "العنوان", "Title",
                "الرسالة", "Message", "INFO", "INFO", null
        );

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = notificationService.markAsRead("notif-1", "admin");

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }
}
