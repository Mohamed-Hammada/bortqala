package com.bemo.hr.growth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "party_id", nullable = false, length = 36)
    private String partyId;
    @Column(name = "points_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal pointsBalance = BigDecimal.ZERO;
    @Column(name = "total_earned", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEarned = BigDecimal.ZERO;
    @Column(name = "total_redeemed", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRedeemed = BigDecimal.ZERO;
    @Column(name = "total_expired", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpired = BigDecimal.ZERO;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected LoyaltyAccount() {}

    public LoyaltyAccount(String partyId) {
        this.id = UUID.randomUUID().toString();
        this.partyId = partyId;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public void credit(BigDecimal points) {
        this.pointsBalance = this.pointsBalance.add(points);
        this.totalEarned = this.totalEarned.add(points);
    }

    public void debit(BigDecimal points) {
        if (this.pointsBalance.compareTo(points) < 0) {
            throw new IllegalStateException("Insufficient points balance.");
        }
        this.pointsBalance = this.pointsBalance.subtract(points);
        this.totalRedeemed = this.totalRedeemed.add(points);
    }

    public void expire(BigDecimal points) {
        this.pointsBalance = this.pointsBalance.subtract(points);
        this.totalExpired = this.totalExpired.add(points);
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getPartyId() { return partyId; }
    public BigDecimal getPointsBalance() { return pointsBalance; }
    public BigDecimal getTotalEarned() { return totalEarned; }
    public BigDecimal getTotalRedeemed() { return totalRedeemed; }
    public BigDecimal getTotalExpired() { return totalExpired; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
}
