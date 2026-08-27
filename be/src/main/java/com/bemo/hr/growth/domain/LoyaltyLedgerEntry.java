package com.bemo.hr.growth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loyalty_ledger")
public class LoyaltyLedgerEntry {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "loyalty_account_id", nullable = false, length = 36)
    private String loyaltyAccountId;
    @Column(name = "party_id", nullable = false, length = 36)
    private String partyId;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal points;
    @Column(name = "running_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal runningBalance;
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    @Column(name = "reference_id", length = 36)
    private String referenceId;
    @Column(name = "rule_snapshot", length = 500)
    private String ruleSnapshot;
    @Column(length = 500)
    private String notes;
    @Column(name = "actor", nullable = false, length = 100)
    private String actor;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected LoyaltyLedgerEntry() {}

    public LoyaltyLedgerEntry(String loyaltyAccountId, String partyId, String type,
                              BigDecimal points, BigDecimal runningBalance,
                              String referenceType, String referenceId,
                              String ruleSnapshot, String notes, String actor) {
        this.id = UUID.randomUUID().toString();
        this.loyaltyAccountId = loyaltyAccountId;
        this.partyId = partyId;
        this.type = type;
        this.points = points;
        this.runningBalance = runningBalance;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.ruleSnapshot = ruleSnapshot;
        this.notes = notes;
        this.actor = actor;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getLoyaltyAccountId() { return loyaltyAccountId; }
    public String getPartyId() { return partyId; }
    public String getType() { return type; }
    public BigDecimal getPoints() { return points; }
    public BigDecimal getRunningBalance() { return runningBalance; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getRuleSnapshot() { return ruleSnapshot; }
    public String getNotes() { return notes; }
    public String getActor() { return actor; }
    public long getCreatedAt() { return createdAt; }
}
