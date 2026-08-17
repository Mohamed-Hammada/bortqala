package com.bemo.hr.notification.push;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "web_push_subscriptions")
@Getter
public class WebPushSubscription {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;

    @Column(name = "username", nullable = false, length = 160)
    private String username;

    @Column(name = "endpoint", nullable = false, length = 2000)
    private String endpoint;

    @Column(name = "endpoint_hash", nullable = false, length = 64)
    private String endpointHash;

    @Column(name = "p256dh_key", nullable = false, length = 512)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 256)
    private String authKey;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "push_approvals", nullable = false)
    private boolean pushApprovals;

    @Column(name = "push_payroll", nullable = false)
    private boolean pushPayroll;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebPushSubscription() {
    }

    public WebPushSubscription(
            String username,
            String endpoint,
            String endpointHash,
            String p256dhKey,
            String authKey,
            String locale,
            boolean pushApprovals,
            boolean pushPayroll) {
        this.id = UUID.randomUUID().toString();
        update(username, endpoint, endpointHash, p256dhKey, authKey, locale, pushApprovals, pushPayroll);
        this.enabled = true;
        this.failureCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeLocale(String locale) {
        return locale != null && locale.toLowerCase(Locale.ROOT).startsWith("en") ? "en-US" : "ar-EG";
    }

    public void update(
            String username,
            String endpoint,
            String endpointHash,
            String p256dhKey,
            String authKey,
            String locale,
            boolean pushApprovals,
            boolean pushPayroll) {
        this.username = normalizeUsername(username);
        this.endpoint = endpoint;
        this.endpointHash = endpointHash;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
        this.locale = normalizeLocale(locale);
        this.pushApprovals = pushApprovals;
        this.pushPayroll = pushPayroll;
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public boolean allows(String notificationType) {
        String type = notificationType == null ? "" : notificationType.toUpperCase(Locale.ROOT);
        if (type.contains("APPROVAL")) return pushApprovals;
        if (type.contains("PAYROLL")) return pushPayroll;
        return true;
    }

    public void markSuccess() {
        this.failureCount = 0;
        this.lastSuccessAt = Instant.now();
        this.updatedAt = this.lastSuccessAt;
    }

    public void markFailure() {
        this.failureCount++;
        this.lastFailureAt = Instant.now();
        this.updatedAt = this.lastFailureAt;
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public boolean belongsTo(String username) {
        return this.username.equalsIgnoreCase(normalizeUsername(username));
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
}
