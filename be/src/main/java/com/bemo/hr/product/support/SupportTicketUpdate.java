package com.bemo.hr.product.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_ticket_updates")
@Getter
public class SupportTicketUpdate {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "ticket_id", nullable = false)
    private String ticketId;
    @Column(name = "from_status", length = 30)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;
    @Column(nullable = false, length = 2000)
    private String comment;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SupportTicketUpdate() {
    }

    public SupportTicketUpdate(String ticket, String from, String to, String comment, String actor, String operation) {
        id = UUID.randomUUID().toString();
        ticketId = ticket;
        fromStatus = from;
        toStatus = to;
        this.comment = comment;
        this.actor = actor;
        operationId = operation;
        createdAt = Instant.now();
    }
}
