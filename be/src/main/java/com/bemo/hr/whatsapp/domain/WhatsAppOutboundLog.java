package com.bemo.hr.whatsapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_outbound_log")
public class WhatsAppOutboundLog {

    public enum Status { QUEUED, SENT, DELIVERED, FAILED, NO_CONSENT }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "recipient_type", nullable = false, length = 20)
    private String recipientType;
    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;
    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;
    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;
    @Column(name = "params", columnDefinition = "text")
    private String params;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "dedupe_key", length = 200)
    private String dedupeKey;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Version
    private Long version;

    protected WhatsAppOutboundLog() {}

    public WhatsAppOutboundLog(String appId, String recipientType, String recipientId,
                               String phoneNumber, String templateKey, String params,
                               String dedupeKey) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.phoneNumber = phoneNumber;
        this.templateKey = templateKey;
        this.params = params;
        this.status = Status.QUEUED.name();
        this.retryCount = 0;
        this.dedupeKey = dedupeKey;
    }

    public void markSent(String providerMessageId) {
        this.status = Status.SENT.name();
        this.providerMessageId = providerMessageId;
        this.sentAt = Instant.now();
    }

    public void markDelivered() {
        this.status = Status.DELIVERED.name();
        this.deliveredAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = Status.FAILED.name();
        this.errorMessage = error;
        this.retryCount++;
    }

    public void markNoConsent() {
        this.status = Status.NO_CONSENT.name();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRecipientType() { return recipientType; }
    public String getRecipientId() { return recipientId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getTemplateKey() { return templateKey; }
    public String getParams() { return params; }
    public Status getStatus() { return Status.valueOf(status); }
    public String getProviderMessageId() { return providerMessageId; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public String getDedupeKey() { return dedupeKey; }
    public Instant getSentAt() { return sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }
}
