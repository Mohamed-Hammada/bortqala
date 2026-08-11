package com.bemo.hr.workforce;

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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_disputes")
public class WorkforceDispute {

    public enum Status {
        DRAFT, UNDER_REVIEW, RESOLVED, REJECTED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "settlement_period_id", nullable = false, length = 36)
    private String settlementPeriodId;

    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;

    @Column(name = "disputed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal disputedAmount;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceDispute() {}

    public WorkforceDispute(String settlementPeriodId, String contractorId, BigDecimal disputedAmount, String reason) {
        this.id = UUID.randomUUID().toString();
        this.settlementPeriodId = settlementPeriodId;
        this.contractorId = contractorId;
        this.disputedAmount = disputedAmount;
        this.reason = reason;
        this.status = Status.DRAFT;
    }

    public void submitForReview() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT disputes can be submitted for review");
        }
        this.status = Status.UNDER_REVIEW;
    }

    public void resolve(String resolutionNotes, String username) {
        if (this.status != Status.UNDER_REVIEW) {
            throw new IllegalStateException("Only UNDER_REVIEW disputes can be resolved");
        }
        this.status = Status.RESOLVED;
        this.resolutionNotes = resolutionNotes;
        this.resolvedBy = username;
    }

    public void reject(String reason, String username) {
        if (this.status != Status.UNDER_REVIEW) {
            throw new IllegalStateException("Only UNDER_REVIEW disputes can be rejected");
        }
        this.status = Status.REJECTED;
        this.resolutionNotes = reason;
        this.resolvedBy = username;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSettlementPeriodId() { return settlementPeriodId; }
    public String getContractorId() { return contractorId; }
    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public String getReason() { return reason; }
    public Status getStatus() { return status; }
    public String getResolutionNotes() { return resolutionNotes; }
    public String getResolvedBy() { return resolvedBy; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
