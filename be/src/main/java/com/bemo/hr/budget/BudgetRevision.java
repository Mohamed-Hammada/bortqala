package com.bemo.hr.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_revisions")
public class BudgetRevision {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "budget_id", nullable = false, length = 36)
    private String budgetId;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "previous_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal previousAmount;

    @Column(name = "new_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal newAmount;

    @Column(length = 500)
    private String reason;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "decided_at")
    private Long decidedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected BudgetRevision() {}

    public BudgetRevision(String budgetId, int revisionNumber, BigDecimal previousAmount, BigDecimal newAmount,
                          String reason, String requestedBy, boolean approvalRequired) {
        this.id = UUID.randomUUID().toString();
        this.budgetId = budgetId;
        this.revisionNumber = revisionNumber;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.reason = reason.strip();
        this.requestedBy = requestedBy;
        this.status = approvalRequired ? Status.PENDING : Status.APPROVED;
        if (!approvalRequired) {
            this.approvedBy = requestedBy;
            this.decidedAt = System.currentTimeMillis();
        }
    }

    public void approve(String actor) {
        requirePending();
        this.status = Status.APPROVED;
        this.approvedBy = actor;
        this.decidedAt = System.currentTimeMillis();
    }

    public void reject(String actor) {
        requirePending();
        this.status = Status.REJECTED;
        this.approvedBy = actor;
        this.decidedAt = System.currentTimeMillis();
    }

    private void requirePending() {
        if (status != Status.PENDING) throw new IllegalStateException("Budget revision is not pending.");
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getBudgetId() { return budgetId; }
    public int getRevisionNumber() { return revisionNumber; }
    public BigDecimal getPreviousAmount() { return previousAmount; }
    public BigDecimal getNewAmount() { return newAmount; }
    public String getReason() { return reason; }
    public String getRequestedBy() { return requestedBy; }
    public Status getStatus() { return status; }
    public String getApprovedBy() { return approvedBy; }
    public Long getDecidedAt() { return decidedAt; }
    public long getCreatedAt() { return createdAt; }
}
