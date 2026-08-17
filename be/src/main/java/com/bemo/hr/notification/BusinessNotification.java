package com.bemo.hr.notification;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "business_notifications")
@Getter
public class BusinessNotification {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "recipient_username", nullable = false, length = 160)
    private String recipientUsername;
    @Column(name = "title_ar", nullable = false, length = 255)
    private String titleAr;
    @Column(name = "title_en", nullable = false, length = 255)
    private String titleEn;
    @Column(name = "message_ar", nullable = false, length = 1000)
    private String messageAr;
    @Column(name = "message_en", nullable = false, length = 1000)
    private String messageEn;
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;
    @Column(name = "action_link", length = 500)
    private String actionLink;
    @Column(name = "exception_key", length = 100)
    private String exceptionKey;
    @Column(name = "impact_ar", length = 500)
    private String impactAr;
    @Column(name = "impact_en", length = 500)
    private String impactEn;
    @Column(name = "reason_ar", length = 500)
    private String reasonAr;
    @Column(name = "reason_en", length = 500)
    private String reasonEn;
    @Column(name = "recommendation_ar", length = 500)
    private String recommendationAr;
    @Column(name = "recommendation_en", length = 500)
    private String recommendationEn;
    @Column(name = "impact_amount", precision = 19, scale = 4)
    private BigDecimal impactAmount;
    @Column(name = "impact_currency", length = 3)
    private String impactCurrency;
    @Column(name = "action_label_key", length = 120)
    private String actionLabelKey;
    @Column(name = "role_targets", length = 500)
    private String roleTargets;
    @Column(name = "is_read", nullable = false)
    private boolean isRead;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BusinessNotification() {
    }

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

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public void enrich(String exceptionKey, String impactAr, String impactEn, String reasonAr, String reasonEn, String recommendationAr, String recommendationEn, BigDecimal impactAmount, String impactCurrency, String actionLabelKey, List<String> roleTargets) {
        this.exceptionKey = blank(exceptionKey);
        this.impactAr = blank(impactAr);
        this.impactEn = blank(impactEn);
        this.reasonAr = blank(reasonAr);
        this.reasonEn = blank(reasonEn);
        this.recommendationAr = blank(recommendationAr);
        this.recommendationEn = blank(recommendationEn);
        this.impactAmount = impactAmount;
        this.impactCurrency = impactCurrency == null ? null : impactCurrency.strip().toUpperCase();
        this.actionLabelKey = blank(actionLabelKey);
        this.roleTargets = roleTargets == null ? null : roleTargets.stream().filter(java.util.Objects::nonNull).map(v -> v.replace("ROLE_", "").strip().toUpperCase()).filter(v -> !v.isBlank()).distinct().sorted().collect(java.util.stream.Collectors.joining(","));
        if (this.roleTargets != null && this.roleTargets.isBlank()) this.roleTargets = null;
        if (actionLink != null && !actionLink.startsWith("/")) actionLink = null;
    }

    public List<String> targetRoles() {
        return roleTargets == null || roleTargets.isBlank() ? List.of() : List.of(roleTargets.split(","));
    }

    public void markRead() {
        this.isRead = true;
        this.readAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
