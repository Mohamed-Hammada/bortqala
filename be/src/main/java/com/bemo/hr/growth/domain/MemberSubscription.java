package com.bemo.hr.growth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "member_subscriptions")
public class MemberSubscription {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "party_id", nullable = false, length = 36)
    private String partyId;
    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;
    @Column(name = "start_date", nullable = false)
    private long startDate;
    @Column(name = "current_period_end", nullable = false)
    private long currentPeriodEnd;
    @Column(name = "next_invoice_date")
    private Long nextInvoiceDate;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "cancelled_at")
    private Long cancelledAt;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected MemberSubscription() {}

    public MemberSubscription(String partyId, String planId, long startDate, long currentPeriodEnd, String status) {
        this.id = UUID.randomUUID().toString();
        this.partyId = partyId;
        this.planId = planId;
        this.startDate = startDate;
        this.currentPeriodEnd = currentPeriodEnd;
        this.status = status;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public void activate(long newPeriodEnd) {
        this.status = "ACTIVE";
        this.currentPeriodEnd = newPeriodEnd;
    }

    public void enterGrace(long graceDeadline) {
        this.status = "GRACE";
        this.currentPeriodEnd = graceDeadline;
    }

    public void expire() { this.status = "EXPIRED"; }

    public void cancel() {
        this.status = "CANCELLED";
        this.cancelledAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getPartyId() { return partyId; }
    public String getPlanId() { return planId; }
    public long getStartDate() { return startDate; }
    public long getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Long getNextInvoiceDate() { return nextInvoiceDate; }
    public String getStatus() { return status; }
    public Long getCancelledAt() { return cancelledAt; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public void setNextInvoiceDate(Long nextInvoiceDate) { this.nextInvoiceDate = nextInvoiceDate; }
    public void setStatus(String status) { this.status = status; }
}
