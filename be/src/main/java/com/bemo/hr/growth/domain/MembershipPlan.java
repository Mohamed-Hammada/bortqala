package com.bemo.hr.growth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
    @Column(name = "period_days", nullable = false)
    private int periodDays;
    @Column(name = "grace_days", nullable = false)
    private int graceDays;
    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "loyalty_earn_rate", precision = 10, scale = 4)
    private BigDecimal loyaltyEarnRate;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected MembershipPlan() {}

    public MembershipPlan(String name, String nameEn, BigDecimal price, String currencyCode,
                          int periodDays, int graceDays, boolean autoRenew, BigDecimal loyaltyEarnRate) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.nameEn = nameEn;
        this.price = price;
        this.currencyCode = currencyCode;
        this.periodDays = periodDays;
        this.graceDays = graceDays;
        this.autoRenew = autoRenew;
        this.loyaltyEarnRate = loyaltyEarnRate;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public BigDecimal getPrice() { return price; }
    public String getCurrencyCode() { return currencyCode; }
    public int getPeriodDays() { return periodDays; }
    public int getGraceDays() { return graceDays; }
    public boolean isAutoRenew() { return autoRenew; }
    public boolean isActive() { return active; }
    public BigDecimal getLoyaltyEarnRate() { return loyaltyEarnRate; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
}
