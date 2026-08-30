package com.bemo.hr.marketing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "campaign_recipients")
public class CampaignRecipient {

    public enum Status { QUEUED, SENT, FAILED, BOUNCED }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "campaign_id", nullable = false, length = 36)
    private String campaignId;
    @Column(name = "target_ref", length = 100)
    private String targetRef;
    @Column(name = "email", length = 255)
    private String email;
    @Column(name = "phone", length = 50)
    private String phone;
    @Column(name = "locale", length = 10)
    private String locale;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "sent_at")
    private Long sentAtEpochMs;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected CampaignRecipient() {}

    public CampaignRecipient(String appId, String campaignId, String targetRef,
                             String email, String phone, String locale) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.campaignId = campaignId;
        this.targetRef = targetRef;
        this.email = email;
        this.phone = phone;
        this.locale = locale;
        this.status = Status.QUEUED.name();
    }

    public void markSent(long sentAt) {
        this.status = Status.SENT.name();
        this.sentAtEpochMs = sentAt;
    }

    public void markFailed(String error) {
        this.status = Status.FAILED.name();
        this.errorMessage = error;
    }

    public void markBounced(String error) {
        this.status = Status.BOUNCED.name();
        this.errorMessage = error;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getCampaignId() { return campaignId; }
    public String getTargetRef() { return targetRef; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLocale() { return locale; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Long getSentAtEpochMs() { return sentAtEpochMs; }
    public long getCreatedAt() { return createdAt; }
}
