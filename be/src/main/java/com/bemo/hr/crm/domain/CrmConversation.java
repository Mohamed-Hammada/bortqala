package com.bemo.hr.crm.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_conversations")
public class CrmConversation {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 30, nullable = false)
    private CrmChannelType channelType;

    @Column(name = "external_sender_id", length = 100, nullable = false)
    private String externalSenderId;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "lead_id", length = 36)
    private String leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private CrmConversationStatus status;

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;

    @Column(name = "assigned_agent_id", length = 36)
    private String assignedAgentId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected CrmConversation() {}

    public CrmConversation(CrmChannelType channelType, String externalSenderId,
                           String senderName, String leadId) {
        this.id = UUID.randomUUID().toString();
        this.channelType = channelType;
        this.externalSenderId = externalSenderId;
        this.senderName = senderName;
        this.leadId = leadId;
        this.status = CrmConversationStatus.OPEN;
        this.unreadCount = 0;
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordInboundMessage(String messagePreview, boolean botHandled) {
        this.lastMessagePreview = messagePreview != null && messagePreview.length() > 250
                ? messagePreview.substring(0, 250) + "..." : messagePreview;
        this.unreadCount += 1;
        this.status = botHandled ? CrmConversationStatus.BOT_HANDLED : CrmConversationStatus.OPEN;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void recordOutboundMessage(String messagePreview) {
        this.lastMessagePreview = messagePreview != null && messagePreview.length() > 250
                ? messagePreview.substring(0, 250) + "..." : messagePreview;
        this.unreadCount = 0;
        this.status = CrmConversationStatus.AGENT_HANDLED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void resetUnread() {
        this.unreadCount = 0;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void linkLead(String leadId) {
        this.leadId = leadId;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void assignAgent(String agentId) {
        this.assignedAgentId = agentId;
        this.status = CrmConversationStatus.AGENT_HANDLED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void resolve() {
        this.status = CrmConversationStatus.RESOLVED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public CrmChannelType getChannelType() { return channelType; }
    public String getExternalSenderId() { return externalSenderId; }
    public String getSenderName() { return senderName; }
    public String getLeadId() { return leadId; }
    public CrmConversationStatus getStatus() { return status; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public int getUnreadCount() { return unreadCount; }
    public String getAssignedAgentId() { return assignedAgentId; }
    public Long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
