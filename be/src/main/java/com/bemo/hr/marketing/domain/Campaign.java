package com.bemo.hr.marketing.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    public enum Channel { EMAIL, SMS, WHATSAPP }
    public enum Status { DRAFT, SCHEDULED, SENDING, SENT, FAILED }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 20)
    private String channel;
    @Column(name = "subject", length = 500)
    private String subject;
    @Column(name = "body_ar", length = 4000)
    private String bodyAr;
    @Column(name = "body_en", length = 4000)
    private String bodyEn;
    @Column(name = "segment_snapshot", length = 4000)
    private String segmentSnapshot;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "scheduled_at")
    private Long scheduledAtEpochMs;
    @Column(name = "total_recipients")
    private int totalRecipients;
    @Column(name = "sent_count")
    private int sentCount;
    @Column(name = "failed_count")
    private int failedCount;
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected Campaign() {}

    public Campaign(String appId, String name, Channel channel, String subject,
                    String bodyAr, String bodyEn, String segmentSnapshot) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.name = name;
        this.channel = channel.name();
        this.subject = subject;
        this.bodyAr = bodyAr;
        this.bodyEn = bodyEn;
        this.segmentSnapshot = segmentSnapshot;
        this.status = Status.DRAFT.name();
    }

    public void startSending(int total) {
        if (status != Status.DRAFT.name() && status != Status.SCHEDULED.name())
            throw new BusinessRuleException("Campaign cannot be sent from current status.",
                    "CAMPAIGN_INVALID_STATE", HttpStatus.CONFLICT);
        this.status = Status.SENDING.name();
        this.totalRecipients = total;
    }

    public void markSent() {
        this.status = Status.SENT.name();
    }

    public void markFailed(String error) {
        this.status = Status.FAILED.name();
        this.errorMessage = error;
    }

    public void incrementSent() { this.sentCount++; }
    public void incrementFailed() { this.failedCount++; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public String getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBodyAr() { return bodyAr; }
    public String getBodyEn() { return bodyEn; }
    public String getSegmentSnapshot() { return segmentSnapshot; }
    public String getStatus() { return status; }
    public Long getScheduledAtEpochMs() { return scheduledAtEpochMs; }
    public int getTotalRecipients() { return totalRecipients; }
    public int getSentCount() { return sentCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public long getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public void setScheduledAtEpochMs(Long v) { this.scheduledAtEpochMs = v; }
    public void setName(String v) { this.name = v; }
    public void setSubject(String v) { this.subject = v; }
    public void setBodyAr(String v) { this.bodyAr = v; }
    public void setBodyEn(String v) { this.bodyEn = v; }
}
