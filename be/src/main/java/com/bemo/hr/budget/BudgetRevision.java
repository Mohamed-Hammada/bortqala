package com.bemo.hr.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_revisions")
public class BudgetRevision {

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

    @Column(name = "approved_by", nullable = false, length = 100)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected BudgetRevision() {}

    public BudgetRevision(String budgetId, int revisionNumber, BigDecimal previousAmount, BigDecimal newAmount, String reason, String approvedBy) {
        this.id = UUID.randomUUID().toString();
        this.budgetId = budgetId;
        this.revisionNumber = revisionNumber;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.reason = reason;
        this.approvedBy = approvedBy;
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
    public String getApprovedBy() { return approvedBy; }
    public long getCreatedAt() { return createdAt; }
}
