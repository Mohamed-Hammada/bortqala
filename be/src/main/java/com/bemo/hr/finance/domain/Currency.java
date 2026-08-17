package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "currencies")
public class Currency {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(name = "is_base", nullable = false)
    private boolean isBase;

    @Column(name = "exchange_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal exchangeRate;

    /*
     * The following fields are deliberately separate from exchange_rate.
     * They are informational hints fetched from Frankfurter and MUST NOT
     * silently change the configured accounting rate.
     */
    @Column(name = "reference_exchange_rate", precision = 20, scale = 8)
    private BigDecimal referenceExchangeRate;

    @Column(name = "reference_rate_provider", length = 30)
    private String referenceRateProvider;

    @Column(name = "reference_rate_base_code", length = 10)
    private String referenceRateBaseCode;

    @Column(name = "reference_rate_date")
    private LocalDate referenceRateDate;

    @Column(name = "reference_rate_fetched_at")
    private Long referenceRateFetchedAt;

    @Column(name = "reference_rate_supported")
    private Boolean referenceRateSupported;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Currency() {
    }

    public Currency(String code, String name, String symbol, boolean isBase, BigDecimal exchangeRate, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, symbol, isBase, exchangeRate, active);
    }

    public void update(String code, String name, String symbol, boolean isBase, BigDecimal exchangeRate, boolean active) {
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.symbol = symbol.strip();
        this.isBase = isBase;
        this.exchangeRate = exchangeRate == null ? BigDecimal.ONE : exchangeRate;
        this.active = active;
    }

    public void updateReferenceRate(String baseCode, BigDecimal rateInBase, LocalDate providerDate, long fetchedAt) {
        this.referenceExchangeRate = rateInBase;
        this.referenceRateProvider = "FRANKFURTER";
        this.referenceRateBaseCode = baseCode == null ? null : baseCode.strip().toUpperCase();
        this.referenceRateDate = providerDate;
        this.referenceRateFetchedAt = fetchedAt;
        this.referenceRateSupported = Boolean.TRUE;
    }

    public void markReferenceUnavailable(String baseCode, long fetchedAt, boolean supported) {
        this.referenceExchangeRate = null;
        this.referenceRateProvider = "FRANKFURTER";
        this.referenceRateBaseCode = baseCode == null ? null : baseCode.strip().toUpperCase();
        this.referenceRateDate = null;
        this.referenceRateFetchedAt = fetchedAt;
        this.referenceRateSupported = supported;
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isBase() {
        return isBase;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public BigDecimal getReferenceExchangeRate() {
        return referenceExchangeRate;
    }

    public String getReferenceRateProvider() {
        return referenceRateProvider;
    }

    public String getReferenceRateBaseCode() {
        return referenceRateBaseCode;
    }

    public LocalDate getReferenceRateDate() {
        return referenceRateDate;
    }

    public Long getReferenceRateFetchedAt() {
        return referenceRateFetchedAt;
    }

    public Boolean getReferenceRateSupported() {
        return referenceRateSupported;
    }

    public boolean isActive() {
        return active;
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
}
