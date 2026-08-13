package com.bemo.hr.workforce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "workforce_request_approvals")
public class WorkforceRequestApproval {

    public enum Decision {
        APPROVED, REJECTED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "approver_user_id", nullable = false, length = 100)
    private String approverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Decision decision;

    @Column(length = 255)
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private long decidedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceRequestApproval() {}

    public WorkforceRequestApproval(String requestId, String approverUserId, Decision decision, String comment) {
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.approverUserId = approverUserId;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRequestId() { return requestId; }
    public String getApproverUserId() { return approverUserId; }
    public Decision getDecision() { return decision; }
    public String getComment() { return comment; }
    public long getDecidedAt() { return decidedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
