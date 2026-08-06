package com.bemo.hr.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_notifications")
@Getter
public class BusinessNotification {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "recipient_username", nullable = false, length = 160) private String recipientUsername;
    @Column(name = "title_ar", nullable = false, length = 255) private String titleAr;
    @Column(name = "title_en", nullable = false, length = 255) private String titleEn;
    @Column(name = "message_ar", nullable = false, length = 1000) private String messageAr;
    @Column(name = "message_en", nullable = false, length = 1000) private String messageEn;
    @Column(name = "notification_type", nullable = false, length = 50) private String notificationType;
    @Column(name = "priority", nullable = false, length = 20) private String priority;
    @Column(name = "action_link", length = 500) private String actionLink;
    @Column(name = "is_read", nullable = false) private boolean isRead;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected BusinessNotification() { }

    public BusinessNotification(String recipientUsername, String titleAr, String titleEn,
                                String messageAr, String messageEn, String notificationType,
                                String priority, String actionLink) {
        this.id = UUID.randomUUID().toString();
        this.recipientUsername = recipientUsername != null ? recipientUsername.strip().toLowerCase() : "";
        this.titleAr = titleAr;
        this.titleEn = titleEn;
        this.messageAr = messageAr;
        this.messageEn = messageEn;
        this.notificationType = notificationType != null ? notificationType.strip().toUpperCase() : "INFO";
        this.priority = priority != null ? priority.strip().toUpperCase() : "INFO";
        this.actionLink = actionLink;
        this.isRead = false;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.isRead = true;
        this.readAt = Instant.now();
    }

    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
