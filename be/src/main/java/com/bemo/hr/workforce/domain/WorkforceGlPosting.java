package com.bemo.hr.workforce.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_gl_postings")
public class WorkforceGlPosting {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "settlement_id", nullable = false, length = 36)
    private String settlementId;
    @Column(name = "journal_id", nullable = false, length = 36)
    private String journalId;
    @Column(name = "posted_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal postedAmount;
    @Column(name = "posted_at", nullable = false)
    private long postedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.POSTED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceGlPosting() {
    }

    public WorkforceGlPosting(String settlementId, String journalId, BigDecimal postedAmount) {
        this.id = UUID.randomUUID().toString();
        this.settlementId = settlementId;
        this.journalId = journalId;
        this.postedAmount = postedAmount;
        this.postedAt = System.currentTimeMillis();
        this.status = Status.POSTED;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getSettlementId() {
        return settlementId;
    }

    public String getJournalId() {
        return journalId;
    }

    public BigDecimal getPostedAmount() {
        return postedAmount;
    }

    public long getPostedAt() {
        return postedAt;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        POSTED
    }
}
