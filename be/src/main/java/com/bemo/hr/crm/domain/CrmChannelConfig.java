package com.bemo.hr.crm.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_channel_configs")
public class CrmChannelConfig {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 30, nullable = false)
    private CrmChannelType channelType;

    @Column(name = "channel_name", length = 100, nullable = false)
    private String channelName;

    @Column(name = "account_identifier", length = 100)
    private String accountIdentifier;

    @Column(name = "masked_api_token", length = 255)
    private String maskedApiToken;

    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Column(name = "bot_enabled", nullable = false)
    private boolean botEnabled;

    @Column(name = "auto_reply_greeting", length = 500)
    private String autoReplyGreeting;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected CrmChannelConfig() {}

    public CrmChannelConfig(CrmChannelType channelType, String channelName, String accountIdentifier,
                            String maskedApiToken, String webhookSecret, boolean botEnabled,
                            String autoReplyGreeting, boolean active) {
        this.id = UUID.randomUUID().toString();
        this.channelType = channelType;
        this.channelName = channelName;
        this.accountIdentifier = accountIdentifier;
        this.maskedApiToken = maskedApiToken;
        this.webhookSecret = webhookSecret;
        this.botEnabled = botEnabled;
        this.autoReplyGreeting = autoReplyGreeting;
        this.active = active;
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String channelName, String accountIdentifier, String maskedApiToken,
                       String webhookSecret, boolean botEnabled, String autoReplyGreeting, boolean active) {
        this.channelName = channelName;
        this.accountIdentifier = accountIdentifier;
        if (maskedApiToken != null && !maskedApiToken.isBlank()) {
            this.maskedApiToken = maskedApiToken;
        }
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            this.webhookSecret = webhookSecret;
        }
        this.botEnabled = botEnabled;
        this.autoReplyGreeting = autoReplyGreeting;
        this.active = active;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public CrmChannelType getChannelType() { return channelType; }
    public String getChannelName() { return channelName; }
    public String getAccountIdentifier() { return accountIdentifier; }
    public String getMaskedApiToken() { return maskedApiToken; }
    public String getWebhookSecret() { return webhookSecret; }
    public boolean isBotEnabled() { return botEnabled; }
    public String getAutoReplyGreeting() { return autoReplyGreeting; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
