package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_rates")
public class TaxRate {

    public enum Type {
        OUTPUT_VAT,
        INPUT_VAT,
        WITHHOLDING,
        VAT
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "rate_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 30)
    private Type taxType;

    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected TaxRate() {}

    public TaxRate(String code, String name, BigDecimal ratePercentage, Type taxType, String accountId, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, ratePercentage, taxType, accountId, active);
    }

    public void update(String code, String name, BigDecimal ratePercentage, Type taxType, String accountId, boolean active) {
        this.code = code.strip();
        this.name = name.strip();
        this.ratePercentage = ratePercentage == null ? BigDecimal.ZERO : ratePercentage;
        this.taxType = taxType;
        this.accountId = accountId == null || accountId.isBlank() ? null : accountId.strip();
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getRatePercentage() { return ratePercentage; }
    public Type getTaxType() { return taxType; }
    public String getAccountId() { return accountId; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
