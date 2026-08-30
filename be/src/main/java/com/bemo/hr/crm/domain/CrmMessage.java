package com.bemo.hr.crm.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_messages")
public class CrmMessage {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "conversation_id", length = 36, nullable = false)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 20, nullable = false)
    private CrmMessageDirection direction;

    @Column(name = "sender_id", length = 100)
    private String senderId;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "message_text", length = 2000, nullable = false)
    private String messageText;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "delivery_status", length = 30)
    private String deliveryStatus;

    @Column(name = "is_bot_response", nullable = false)
    private boolean isBotResponse;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected CrmMessage() {}

    public CrmMessage(String conversationId, CrmMessageDirection direction, String senderId,
                      String senderName, String messageText, String attachmentUrl,
                      String deliveryStatus, boolean isBotResponse) {
        this.id = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.direction = direction != null ? direction : CrmMessageDirection.INBOUND;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageText = messageText;
        this.attachmentUrl = attachmentUrl;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : "DELIVERED";
        this.isBotResponse = isBotResponse;
        this.createdAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getConversationId() { return conversationId; }
    public CrmMessageDirection getDirection() { return direction; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getMessageText() { return messageText; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public boolean isBotResponse() { return isBotResponse; }
    public long getCreatedAt() { return createdAt; }
}
