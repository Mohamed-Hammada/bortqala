package com.bemo.hr.helpdesk.domain;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    public enum Priority { LOW, NORMAL, HIGH, URGENT }
    public enum Status { NEW, OPEN, WAITING_CUSTOMER, RESOLVED, CLOSED }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "ticket_no", nullable = false)
    private long ticketNo;
    @Column(name = "requester_user_id", length = 100)
    private String requesterUserId;
    @Column(name = "requester_party_id", length = 36)
    private String requesterPartyId;
    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;
    @Column(nullable = false, length = 500)
    private String title;
    @Column(length = 4000)
    private String description;
    @Column(nullable = false, length = 20)
    private String priority;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "assignee_user_id", length = 100)
    private String assigneeUserId;
    @Column(name = "first_response_at")
    private Instant firstResponseAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "due_first_response")
    private Instant dueFirstResponse;
    @Column(name = "due_resolution")
    private Instant dueResolution;
    @Column(name = "sla_breach_first_response")
    private boolean slaBreachFirstResponse;
    @Column(name = "sla_breach_resolution")
    private boolean slaBreachResolution;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected Ticket() {}

    public Ticket(String appId, long ticketNo, String requesterUserId, String categoryId,
                  String title, String description, Priority priority) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.ticketNo = ticketNo;
        this.requesterUserId = requesterUserId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.priority = priority.name();
        this.status = Status.NEW.name();
    }

    public void stampSla(Instant createdAt, int firstResponseHrs, int resolutionHrs, Priority pri) {
        int factor = switch (pri) {
            case URGENT -> 1;
            case HIGH -> 2;
            case NORMAL -> 3;
            case LOW -> 4;
        };
        this.dueFirstResponse = createdAt.plusSeconds((long) firstResponseHrs * factor * 3600);
        this.dueResolution = createdAt.plusSeconds((long) resolutionHrs * factor * 3600);
    }

    public void assign(String assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
        if (status.equals(Status.NEW.name())) this.status = Status.OPEN.name();
    }

    public void transitionTo(Status newStatus) {
        if (this.status.equals(Status.CLOSED.name()) || this.status.equals(Status.RESOLVED.name())) {
            if (newStatus != Status.OPEN && newStatus != Status.WAITING_CUSTOMER)
                throw new BusinessRuleException("Cannot transition from terminal status.",
                        "TICKET_INVALID_TRANSITION", HttpStatus.CONFLICT);
        }
        this.status = newStatus.name();
    }

    public void markFirstResponse(Instant at) {
        if (this.firstResponseAt == null) this.firstResponseAt = at;
    }

    public void resolve(Instant at) {
        this.status = Status.RESOLVED.name();
        this.resolvedAt = at;
    }

    public void setSlaBreachFirstResponse(boolean v) { this.slaBreachFirstResponse = v; }
    public void setSlaBreachResolution(boolean v) { this.slaBreachResolution = v; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public long getTicketNo() { return ticketNo; }
    public String getRequesterUserId() { return requesterUserId; }
    public String getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return Priority.valueOf(priority); }
    public Status getStatus() { return Status.valueOf(status); }
    public String getAssigneeUserId() { return assigneeUserId; }
    public Instant getFirstResponseAt() { return firstResponseAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getDueFirstResponse() { return dueFirstResponse; }
    public Instant getDueResolution() { return dueResolution; }
    public boolean isSlaBreachFirstResponse() { return slaBreachFirstResponse; }
    public boolean isSlaBreachResolution() { return slaBreachResolution; }
    public Long getVersion() { return version; }
    public String getStatusStr() { return status; }
    public String getPriorityStr() { return priority; }
    public long getCreatedAt() { return createdAt; }

    public void setTitle(String t) { this.title = t; }
    public void setDescription(String d) { this.description = d; }
    public void setPriority(Priority p) { this.priority = p.name(); }
}
