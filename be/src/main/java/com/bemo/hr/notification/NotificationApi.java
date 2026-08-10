package com.bemo.hr.notification;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

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
            String exceptionKey,String impactAr,String impactEn,String reasonAr,String reasonEn,
            String recommendationAr,String recommendationEn,BigDecimal impactAmount,String impactCurrency,
            String actionLabelKey,List<String> roleTargets,int priorityScore,
            boolean isRead,
            Long readAt,
            long createdAt
    ) { }

    public record UnreadCountResponse(
            long unreadCount
    ) { }

    public record SendNotificationPayload(
            @NotBlank @Size(max=160) String recipientUsername,
            @NotBlank @Size(max=255) String titleAr,
            @NotBlank @Size(max=255) String titleEn,
            @NotBlank @Size(max=1000) String messageAr,
            @NotBlank @Size(max=1000) String messageEn,
            @Size(max=50) String notificationType,
            @Pattern(regexp="CRITICAL|HIGH|MEDIUM|INFO") String priority,
            @Size(max=500) @Pattern(regexp="^/.*") String actionLink,
            @Size(max=100) @Pattern(regexp="[A-Z0-9_]+") String exceptionKey,
            @Size(max=500) String impactAr,@Size(max=500) String impactEn,@Size(max=500) String reasonAr,@Size(max=500) String reasonEn,
            @Size(max=500) String recommendationAr,@Size(max=500) String recommendationEn,@DecimalMin("0") BigDecimal impactAmount,@Pattern(regexp="[A-Z]{3}") String impactCurrency,
            @Size(max=120) String actionLabelKey,@Size(max=20) List<@Size(max=50) String> roleTargets
    ) { public SendNotificationPayload(String recipientUsername,String titleAr,String titleEn,String messageAr,String messageEn,String notificationType,String priority,String actionLink){this(recipientUsername,titleAr,titleEn,messageAr,messageEn,notificationType,priority,actionLink,null,null,null,null,null,null,null,null,null,null,null);} }
}
