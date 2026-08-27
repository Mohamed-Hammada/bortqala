package com.bemo.hr.growth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "referrals")
public class Referral {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "referrer_party_id", nullable = false, length = 36)
    private String referrerPartyId;
    @Column(name = "referred_party_id", nullable = false, length = 36)
    private String referredPartyId;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "reward_points", precision = 15, scale = 2)
    private BigDecimal rewardPoints;
    @Column(name = "first_purchase_reference_id", length = 36)
    private String firstPurchaseReferenceId;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Referral() {}

    public Referral(String referrerPartyId, String referredPartyId) {
        this.id = UUID.randomUUID().toString();
        this.referrerPartyId = referrerPartyId;
        this.referredPartyId = referredPartyId;
        this.status = "REGISTERED";
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public void markFirstPurchase(String referenceId, BigDecimal rewardPoints) {
        this.status = "FIRST_PURCHASE";
        this.firstPurchaseReferenceId = referenceId;
        this.rewardPoints = rewardPoints;
    }

    public void markRewarded() { this.status = "REWARDED"; }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getReferrerPartyId() { return referrerPartyId; }
    public String getReferredPartyId() { return referredPartyId; }
    public String getStatus() { return status; }
    public BigDecimal getRewardPoints() { return rewardPoints; }
    public String getFirstPurchaseReferenceId() { return firstPurchaseReferenceId; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public void setAppId(String appId) { this.appId = appId; }
}
