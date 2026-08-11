package com.bemo.hr.notification;

import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

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

    @Test
    void actionCenterRanksRoleRelevantExceptionAheadOfGenericCard() {
        var generic=new BusinessNotification("admin","عام","Generic","رسالة","Message","INFO","HIGH","/dashboard");
        var finance=new BusinessNotification("admin","تحصيل","Collection","رسالة","Message","COLLECTION_EXCEPTION","MEDIUM","/sales");
        finance.enrich("OVERDUE_RECEIVABLE","أثر","Impact","سبب","Reason","توصية","Recommendation",new BigDecimal("12000"),"egp","actionCenter.resolve",List.of("FINANCE_MANAGER"));
        when(notificationRepository.findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc("admin")).thenReturn(List.of(generic,finance));
        var result=notificationService.getNotificationsForUser("admin", Set.of("FINANCE_MANAGER"));
        assertThat(result).extracting(NotificationApi.NotificationResponse::exceptionKey).containsExactly("OVERDUE_RECEIVABLE",null);
        assertThat(result.get(0).priorityScore()).isEqualTo(90);
        assertThat(result.get(0).impactAmount()).isEqualByComparingTo("12000");
    }

    @Test
    void advancedCardCarriesReasonRecommendationAndSafeDirectAction() {
        var payload=new NotificationApi.SendNotificationPayload("admin","عنوان","Title","رسالة","Message","ATTENDANCE_EXCEPTION","CRITICAL","/reports/1","MISSING_PUNCH","أثر","Payroll impact","سبب","Missing checkout","توصية","Review evidence",null,null,"actionCenter.review",List.of("HR_MANAGER"));
        when(notificationRepository.save(any())).thenAnswer(invocation->invocation.getArgument(0));
        var result=notificationService.sendNotification(payload,"system");
        assertThat(result.reasonEn()).isEqualTo("Missing checkout");
        assertThat(result.recommendationEn()).isEqualTo("Review evidence");
        assertThat(result.actionLink()).isEqualTo("/reports/1");
        assertThat(result.roleTargets()).containsExactly("HR_MANAGER");
    }

    @Test
    void externalActionLinksAreRejected() {
        var payload=new NotificationApi.SendNotificationPayload("admin","عنوان","Title","رسالة","Message","INFO","INFO","https://example.com",null,null,null,null,null,null,null,null,null,null,List.of());
        when(notificationRepository.save(any())).thenAnswer(invocation->invocation.getArgument(0));
        assertThat(notificationService.sendNotification(payload,"system").actionLink()).isNull();
    }
}
