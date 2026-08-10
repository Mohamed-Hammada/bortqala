package com.bemo.hr.approval;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "approval_delegations") @Getter
public class ApprovalDelegation {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "delegator_user_id", nullable = false, length = 100) private String delegatorUserId;
    @Column(name = "delegate_user_id", nullable = false, length = 100) private String delegateUserId;
    @Column(name = "document_type", length = 50) private String documentType;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_by", nullable = false, length = 100) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected ApprovalDelegation() { }
    public ApprovalDelegation(String delegator, String delegate, String documentType, Instant startsAt,
                              Instant endsAt, String reason, String actor) {
        id = UUID.randomUUID().toString(); delegatorUserId = delegator.strip(); delegateUserId = delegate.strip();
        this.documentType = documentType == null || documentType.isBlank() ? null : documentType.strip().toUpperCase();
        this.startsAt = startsAt; this.endsAt = endsAt; this.reason = reason.strip(); active = true;
        createdBy = actor; createdAt = Instant.now();
    }
    public boolean applies(String delegator, String delegate, String type, Instant now) {
        return active && delegatorUserId.equalsIgnoreCase(delegator) && delegateUserId.equalsIgnoreCase(delegate)
                && (documentType == null || documentType.equalsIgnoreCase(type)) && !now.isBefore(startsAt) && !now.isAfter(endsAt);
    }
    public void deactivate() { active = false; }
}
