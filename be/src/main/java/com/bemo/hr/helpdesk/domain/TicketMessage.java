package com.bemo.hr.helpdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "ticket_messages")
public class TicketMessage {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "ticket_id", nullable = false, length = 36)
    private String ticketId;
    @Column(nullable = false, length = 100)
    private String authorUserId;
    @Column(nullable = false, length = 8000)
    private String body;
    @Column(name = "internal_note", nullable = false)
    private boolean internalNote;
    @Column(name = "attachment_name", length = 255)
    private String attachmentName;
    @Column(name = "attachment_type", length = 100)
    private String attachmentType;
    @Column(name = "attachment_size")
    private Long attachmentSize;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected TicketMessage() {}

    public TicketMessage(String appId, String ticketId, String authorUserId,
                         String body, boolean internalNote) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.ticketId = ticketId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.internalNote = internalNote;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getTicketId() { return ticketId; }
    public String getAuthorUserId() { return authorUserId; }
    public String getBody() { return body; }
    public boolean isInternalNote() { return internalNote; }
    public String getAttachmentName() { return attachmentName; }
    public String getAttachmentType() { return attachmentType; }
    public Long getAttachmentSize() { return attachmentSize; }
    public long getCreatedAt() { return createdAt; }

    public void setAttachment(String name, String type, Long size) {
        this.attachmentName = name;
        this.attachmentType = type;
        this.attachmentSize = size;
    }
}
