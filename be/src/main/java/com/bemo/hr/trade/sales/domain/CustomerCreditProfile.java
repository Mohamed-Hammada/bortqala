package com.bemo.hr.trade.sales.domain;

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
@Table(name = "customer_credit_profiles")
public class CustomerCreditProfile {

    public enum Status {
        ACTIVE, HOLD, EXCEEDED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays = 30;

    @Column(name = "credit_hold", nullable = false)
    private boolean creditHold = false;

    @Column(name = "current_exposure", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentExposure = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CustomerCreditProfile() {}

    public CustomerCreditProfile(String customerId) {
        this(customerId, BigDecimal.ZERO);
    }

    public CustomerCreditProfile(String customerId, BigDecimal creditLimit) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.creditLimit = creditLimit;
        this.paymentTermsDays = 30;
        this.creditHold = false;
        this.currentExposure = BigDecimal.ZERO;
        this.status = Status.ACTIVE;
    }

    public void update(BigDecimal creditLimit, int paymentTermsDays, boolean creditHold) {
        this.creditLimit = creditLimit == null ? BigDecimal.ZERO : creditLimit;
        this.paymentTermsDays = Math.max(0, paymentTermsDays);
        this.creditHold = creditHold;
        reevaluateStatus();
    }

    public void setCreditLimit(BigDecimal newLimit) {
        this.creditLimit = newLimit == null ? BigDecimal.ZERO : newLimit;
        reevaluateStatus();
    }

    public void increaseExposure(BigDecimal amount) {
        this.currentExposure = this.currentExposure.add(amount);
        reevaluateStatus();
    }

    public void reduceExposure(BigDecimal amount) {
        this.currentExposure = this.currentExposure.subtract(amount).max(BigDecimal.ZERO);
        reevaluateStatus();
    }

    private void reevaluateStatus() {
        if (this.creditHold) {
            this.status = Status.HOLD;
        } else if (this.currentExposure.compareTo(this.creditLimit) > 0) {
            this.status = Status.EXCEEDED;
        } else {
            this.status = Status.ACTIVE;
        }
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public int getPaymentTermsDays() { return paymentTermsDays; }
    public boolean isCreditHold() { return creditHold; }
    public BigDecimal getCurrentExposure() { return currentExposure; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
